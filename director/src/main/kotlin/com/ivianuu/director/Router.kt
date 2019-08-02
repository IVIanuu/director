/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director

import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State.*
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Handles the backStack and delegates the host lifecycle to it's [Controller]s
 */
class Router internal constructor(val parent: Controller? = null) {

    private val _backStack = mutableListOf<RouterTransaction>()
    /**
     * The current backStack
     */
    val backStack: List<RouterTransaction> get() = _backStack

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = false

    /**
     * Whether or not this router is started
     */
    var isStarted = false
        private set

    /**
     * Whether or not this router has been destroyed
     */
    var isDestroyed = false
        private set

    /**
     * The container of this router
     */
    var container: ViewGroup? = null
        private set

    private val _viewModelStoreOwner = RouterViewModelStoreOwner()
    val viewModelStoreOwner: ViewModelStoreOwner get() = _viewModelStoreOwner

    private val changeListeners =
        mutableListOf<ChangeListenerEntry>()

    private val runningHandlers =
        mutableMapOf<Controller, ControllerChangeHandler>()

    private var settingBackStack = false

    /**
     * Sets the backStack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ControllerChangeHandler]
     */
    fun setBackStack(
        newBackStack: List<RouterTransaction>,
        isPush: Boolean,
        handler: ControllerChangeHandler? = null
    ) {
        check(!settingBackStack) {
            "Cannot call setBackStack from within a setBackStack call"
        }

        if (isDestroyed) return

        if (newBackStack == _backStack) return

        settingBackStack = true

        // do not allow pushing the same controller twice
        newBackStack
            .groupBy { it }
            .forEach {
                check(it.value.size == 1) {
                    "Trying to push the same controller to the backStack more than once. $it"
                }
            }

        // do not allow pushing destroyed controllers
        newBackStack.forEach {
            check(it.controller.lifecycle.currentState != DESTROYED) {
                "Trying to push a controller that has already been destroyed $it"
            }
        }

        // do not allow pushing controllers which are already attached to another router
        newBackStack.forEach {
            check(
                !it.isAddedToRouter
                        || !it.controller.lifecycle.currentState.isAtLeast(CREATED)
                        || it.controller.router == this
            ) {
                "Trying to push a controller which is attached to another router $it"
            }
        }

        // Swap around transaction indices to ensure they don't get thrown out of order by the
        // developer rearranging the backStack at runtime.
        val indices = newBackStack
            .onEach { it.ensureValidIndex() }
            .map { it.transactionIndex }
            .sorted()

        newBackStack.forEachIndexed { i, controller ->
            controller.transactionIndex = indices[i]
        }

        val oldBackStack = _backStack.toList()
        val oldVisibleTransactions = oldBackStack.filterVisible()

        _backStack.clear()
        _backStack += newBackStack

        // find destroyed controllers
        val destroyedTransactions = oldBackStack
            .filter { old -> newBackStack.none { it == old } }

        val destroyedInvisibleTransactions = destroyedTransactions
            .filter { it.controller.view == null }

        // Ensure all new controllers have a valid router set
        newBackStack.forEach {
            it.isAddedToRouter = true
            moveControllerToCorrectState(it.controller)
        }

        val newVisibleTransactions = newBackStack.filterVisible()

        if (oldVisibleTransactions != newVisibleTransactions) {
            val oldTopTransaction = oldVisibleTransactions.lastOrNull()
            val newTopTransaction = newVisibleTransactions.lastOrNull()

            // check if we should animate the top controllers
            val replacingTopControllers = newTopTransaction != null && (oldTopTransaction == null
                    || oldTopTransaction != newTopTransaction)

            // Remove all visible controllers which shouldn't be visible anymore
            // from top to bottom
            oldVisibleTransactions
                .dropLast(if (replacingTopControllers) 1 else 0)
                .reversed()
                .filterNot { it in newVisibleTransactions }
                .forEach { transaction ->
                    cancelChange(transaction.controller)

                    val localHandler = handler?.copy()
                        ?: transaction.popChangeHandler?.copy()

                    performControllerChange(
                        from = transaction.controller,
                        to = null,
                        isPush = isPush,
                        handler = localHandler,
                        forceRemoveFromView = true
                    )
                }

            // Add any new controllers to the backStack from bottom to top
            newVisibleTransactions
                .dropLast(if (replacingTopControllers) 1 else 0)
                .filterNot { it in oldVisibleTransactions }
                .forEachIndexed { i, transaction ->
                    val localHandler = handler?.copy() ?: transaction.pushChangeHandler
                    performControllerChange(
                        from = newVisibleTransactions.getOrNull(i - 1)?.controller,
                        to = transaction.controller,
                        isPush = true,
                        handler = localHandler,
                        forceRemoveFromView = false
                    )
                }

            // Replace the old visible top with the new one
            if (replacingTopControllers) {
                val localHandler = handler?.copy()
                    ?: (if (isPush) newTopTransaction?.pushChangeHandler?.copy()
                    else oldTopTransaction?.popChangeHandler?.copy())

                val forceRemoveFromView = oldTopTransaction != null
                        && newBackStack.none { it.controller == oldTopTransaction.controller }

                performControllerChange(
                    from = oldTopTransaction?.controller,
                    to = newTopTransaction?.controller,
                    isPush = isPush,
                    handler = localHandler,
                    forceRemoveFromView = forceRemoveFromView
                )
            }
        }

        // destroy all invisible transactions here
        destroyedInvisibleTransactions.reversed().forEach { it.controller.destroy() }

        settingBackStack = false
    }

    /**
     * Let the current controller handles back click or pops the top controller if possible
     * Returns whether or not the back click was handled
     */
    fun handleBack(): Boolean {
        if (isDestroyed) return false
        val topTransaction = backStack.lastOrNull()

        return if (topTransaction != null) {
            if (topTransaction.controller.handleBack()) {
                true
            } else if (hasRoot && (popsLastView || _backStack.size > 1)) {
                popTop()
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Notifies the [listener] on controller changes
     */
    fun addChangeListener(listener: ControllerChangeListener, recursive: Boolean = false) {
        changeListeners += ChangeListenerEntry(listener, recursive)
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeChangeListener(listener: ControllerChangeListener) {
        changeListeners.removeAll { it.listener == listener }
    }

    internal fun getChangeListeners(recursiveOnly: Boolean = false): List<ControllerChangeListener> {
        return changeListeners
            .filter { !recursiveOnly || it.recursive }
            .map { it.listener } + (parent?.router?.getChangeListeners(true)
            ?: emptyList())
    }

    /**
     * Sets the container of this router
     */
    fun setContainer(container: ViewGroup) {
        this.container = container
        rebind()
    }

    /**
     * Removes the current container if set
     */
    fun removeContainer() {
        endAllChanges()
        _backStack.reversed()
            .map { it.controller }
            .forEach { controller ->
                if (controller.lifecycle.currentState.isAtLeast(STARTED)) {
                    container?.removeView(controller.view!!)
                    controller.detach()
                }

                if (controller.view != null) {
                    controller.destroyView()
                }
            }

        container = null
    }

    fun start() {
        isStarted = true
        // attach visible controllers
        _backStack
            .filterVisible()
            .map { it.controller }
            .filter { it.view?.parent != null }
            .filterNot { it.lifecycle.currentState.isAtLeast(STARTED) }
            .forEach { it.attach() }
    }

    fun stop() {
        isStarted = false

        endAllChanges()

        _backStack
            .reversed()
            .map { it.controller }
            .filter { it.lifecycle.currentState.isAtLeast(STARTED) }
            .forEach { it.detach() }
    }

    fun destroy() {
        isDestroyed = true

        _backStack
            .reversed()
            .forEach { it.controller.destroy() }

        _viewModelStoreOwner.viewModelStore.clear()
    }

    private fun performControllerChange(
        from: Controller?,
        to: Controller?,
        isPush: Boolean,
        handler: ControllerChangeHandler? = null,
        forceRemoveFromView: Boolean
    ) {
        val container = container ?: return
        val listeners = getChangeListeners()

        val handlerToUse = when {
            handler == null -> DefaultChangeHandler()
            handler.hasBeenUsed -> handler.copy()
            else -> handler
        }
        handlerToUse.hasBeenUsed = true

        from?.let { cancelChange(it) }
        to?.let { runningHandlers[it] = handlerToUse }

        listeners.forEach { it.onChangeStarted(this, to, from, isPush, container, handlerToUse) }

        val toView = to?.view ?: to?.createView(container)
        val fromView = from?.view

        val toIndex = getToIndex(to, from, isPush)

        val callback = object : ControllerChangeHandler.Callback {
            override fun addToView() {
                val addingToView = toView != null && toView.parent == null
                val movingToView = toView != null && container.indexOfChild(toView) != toIndex
                if (addingToView) {
                    container.addView(toView, toIndex)
                    attachToController()
                } else if (movingToView) {
                    container.moveView(toView!!, toIndex)
                }
            }

            override fun attachToController() {
                if (isStarted) {
                    to!!.attach()
                }
            }

            override fun removeFromView() {
                if (fromView != null && (!isPush || handlerToUse.removesFromViewOnPush
                            || forceRemoveFromView)
                ) {
                    container.removeView(fromView)
                    detachFromController()
                }
            }

            override fun detachFromController() {
                from!!.detach()
                from.destroyView()
                if (forceRemoveFromView) {
                    from.destroy()
                }
            }

            override fun changeCompleted() {
                if (to != null) {
                    runningHandlers -= to
                }

                listeners.forEach {
                    it.onChangeEnded(this@Router, to, from, isPush, container, handlerToUse)
                }
            }
        }

        val changeData = ChangeData(
            container,
            fromView,
            toView,
            isPush,
            callback,
            toIndex,
            forceRemoveFromView
        )

        handlerToUse.performChange(changeData)
    }

    private fun moveControllerToCorrectState(controller: Controller) {
        if (controller.lifecycle.currentState == INITIALIZED) {
            controller.create(this)
        }

        if (isStarted
            && controller.view?.parent != null
            && !controller.lifecycle.currentState.isAtLeast(STARTED)
        ) {
            controller.attach()
        }

        if (isDestroyed && controller.lifecycle.currentState != DESTROYED) {
            controller.destroy()
        }
    }

    private fun endAllChanges() {
        _backStack.reversed().forEach { cancelChange(it.controller) }
    }

    private fun List<RouterTransaction>.filterVisible(): List<RouterTransaction> {
        for (i in lastIndex downTo 0) {
            val transaction = get(i)

            if (transaction.pushChangeHandler == null
                || transaction.pushChangeHandler!!.removesFromViewOnPush
            ) {
                return drop(i)
            }
        }

        return toList()
    }

    private fun rebind() {
        if (container == null) return

        _backStack
            .filterVisible()
            .forEach {
                performControllerChange(
                    from = null,
                    to = it.controller,
                    isPush = true,
                    handler = DefaultChangeHandler(false),
                    forceRemoveFromView = false
                )
            }
    }

    private fun cancelChange(controller: Controller) {
        runningHandlers.remove(controller)?.cancel()
    }

    private fun getToIndex(
        to: Controller?,
        from: Controller?,
        isPush: Boolean
    ): Int {
        val container = container ?: return -1
        if (to == null) return -1
        return if (isPush || from == null) {
            if (container.childCount == 0) return -1
            val backStackIndex = backStack.indexOfFirst { it.controller == to }
            (0 until container.childCount)
                .map { container.getChildAt(it) }
                .indexOfFirst { v ->
                    backStack.indexOfFirst { it.controller.view == v } > backStackIndex
                }
        } else {
            val currentToIndex = container.indexOfChild(to.view)
            val currentFromIndex = container.indexOfChild(from.view)

            if (currentToIndex == -1 || currentToIndex > currentFromIndex) {
                container.indexOfChild(from.view)
            } else {
                currentToIndex
            }
        }
    }

    private data class ChangeListenerEntry(
        val listener: ControllerChangeListener,
        val recursive: Boolean
    )

    private class RouterViewModelStoreOwner : ViewModelStoreOwner {
        private val _viewModelStore = ViewModelStore()
        override fun getViewModelStore() = _viewModelStore
    }
}

val Router.hasContainer: Boolean get() = container != null

val Router.activity: FragmentActivity?
    get() {
        if (parent != null) return parent.activity

        var context = container?.context
        while (context is ContextWrapper) {
            if (context is FragmentActivity) return context
            context = context.baseContext
        }

        return null
    }

val Router.hasRoot: Boolean get() = backStack.isNotEmpty()

fun Router.findControllerByTag(tag: String): Controller? {
    for (transaction in backStack) {
        if (transaction.tag == tag) {
            return transaction.controller
        }

    }

    return null
}

/**
 * Sets the root Controller. If any [Controller]s are currently in the backStack, they will be removed.
 */
fun Router.setRoot(transaction: RouterTransaction, handler: ControllerChangeHandler? = null) {
    // todo check if we should always use isPush=true
    setBackStack(listOf(transaction), true, handler ?: transaction.pushChangeHandler)
}

/**
 * Pushes a new [Controller] to the backStack
 */
fun Router.push(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackStack = backStack.toMutableList()
    newBackStack += transaction
    setBackStack(newBackStack, true, handler)
}

/**
 * Replaces this router's top [Controller] with the [controller]
 */
fun Router.replaceTop(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackStack = backStack.toMutableList()
    if (newBackStack.isNotEmpty()) newBackStack.removeAt(newBackStack.lastIndex)
    newBackStack += transaction
    setBackStack(newBackStack, true, handler)
}

/**
 * Pops the passed [controller] from the backStack
 */
fun Router.popController(
    controller: Controller,
    handler: ControllerChangeHandler? = null
) {
    backStack.firstOrNull { it.controller == controller }
        ?.let { popTransaction(it, handler) }
}

/**
 * Pops the passed [transaction] from the backStack
 */
fun Router.popTransaction(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackStack = backStack.toMutableList()
    newBackStack -= transaction
    setBackStack(newBackStack, false, handler)
}

/**
 * Pops the top [Controller] from the backStack
 */
fun Router.popTop(handler: ControllerChangeHandler? = null) {
    val controller = backStack.lastOrNull()
        ?: error("Trying to pop the current controller when there are none on the backStack.")
    popTransaction(controller, handler)
}

/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(handler: ControllerChangeHandler? = null) {
    backStack.firstOrNull()?.let { popToTransaction(it, handler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, handler: ControllerChangeHandler? = null) {
    backStack.firstOrNull { it.tag == tag }?.let { popToTransaction(it, handler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popToController(
    controller: Controller,
    handler: ControllerChangeHandler? = null
) {
    backStack
        .firstOrNull { it.controller == controller }
        ?.let { popToTransaction(it, handler) }
}

/**
 * Pops all [Controller]s until the [transaction] is at the top
 */
fun Router.popToTransaction(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackStack = backStack.dropLastWhile { it != transaction }
    setBackStack(newBackStack, false, handler)
}

/**
 * Clears the backStack
 */
fun Router.clear(handler: ControllerChangeHandler? = null) {
    setBackStack(emptyList(), false, handler)
}