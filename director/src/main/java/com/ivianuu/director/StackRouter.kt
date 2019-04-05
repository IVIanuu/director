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

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.stdlibx.takeLastUntil

/**
 * @author Manuel Wrage (IVIanuu)
 */
class StackRouter : Router() {

    override val transactions: List<Transaction> get() = _backstack

    /**
     * The current backstack
     */
    val backstack: List<Transaction> get() = _backstack
    private val _backstack = mutableListOf<Transaction>()

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = DirectorPlugins.defaultPopsLastView

    private val internalControllerListener = ControllerListener(
        postDetach = { controller, _ ->
            if (destroyingControllers.contains(controller)) {
                controller.destroyView(false)
            }

            if (toBeInvisibleControllers.contains(controller)) {
                controller.destroyView(true)
                toBeInvisibleControllers.remove(controller)
            }
        },
        postDestroyView = { controller ->
            if (destroyingControllers.remove(controller)) {
                controller.destroy()
            }
        }
    )

    private val destroyingControllers = mutableListOf<Controller>()
    private val toBeInvisibleControllers = mutableListOf<Controller>()

    override fun onStart() {
        super.onStart()
        _backstack.forEach { it.controller.attach() }
    }

    override fun onContainerSet(container: ViewGroup) {
        super.onContainerSet(container)
        rebind()
    }

    override fun onStop() {
        super.onStop()
        _backstack.reversed().forEach { it.controller.detach() }
    }

    override fun onDestroy() {
        super.onDestroy()
        _backstack.reversed().forEach { it.controller.destroy() }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        _backstack.clear()
        _backstack.addAll(
            savedInstanceState.getParcelableArrayList<Bundle>(KEY_BACKSTACK)!!
                .map { Transaction.fromBundle(it, routerManager.controllerFactory) }
        )

        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        _backstack.forEach { moveControllerToCorrectState(it.controller) }

        rebind()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val backstack = _backstack.map(Transaction::saveInstanceState)
        outState.putParcelableArrayList(KEY_BACKSTACK, ArrayList(backstack))
        outState.putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
    }

    override fun handleBack(): Boolean {
        val currentTransaction = backstack.lastOrNull()

        return if (currentTransaction != null) {
            if (currentTransaction.controller.handleBack()) {
                true
            } else if (hasRoot && (popsLastView || backstackSize > 1)) {
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
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ChangeHandler]
     */
    fun setBackstack(
        newBackstack: List<Transaction>,
        isPush: Boolean,
        handler: ChangeHandler? = null
    ) {
        if (newBackstack == _backstack) return

        // Swap around transaction indices to ensure they don't get thrown out of order by the
        // developer rearranging the backstack at runtime.
        val indices = newBackstack
            .onEach { it.ensureValidIndex(routerManager.transactionIndexer) }
            .map(Transaction::transactionIndex)
            .sorted()

        newBackstack.forEachIndexed { i, transaction ->
            transaction.transactionIndex = indices[i]
        }

        check(newBackstack.size == newBackstack.distinctBy(Transaction::controller).size) {
            "Trying to push the same controller to the backstack more than once."
        }
        newBackstack.forEach {
            check(it.controller.state != ControllerState.DESTROYED) {
                "Trying to push a controller that has already been destroyed ${it.controller.javaClass.simpleName}"
            }
        }

        val oldTransactions = _backstack.toList()
        val oldVisibleTransactions = oldTransactions.filterVisible()

        _backstack.clear()
        _backstack.addAll(newBackstack)

        // find destroyed transactions
        val destroyedTransactions = oldTransactions
            .filter { old -> newBackstack.none { it.controller == old.controller } }

        val (destroyedVisibleTransactions, destroyedInvisibleTransactions) =
            destroyedTransactions
                .partition { it.controller.isAttached }

        destroyingControllers.addAll(destroyedVisibleTransactions.map(Transaction::controller))

        // Ensure all new controllers have a valid router set
        newBackstack.forEach {
            it.attachedToRouter = true
            it.controller.addListener(internalControllerListener)
            moveControllerToCorrectState(it.controller)
        }

        val newVisibleTransactions = newBackstack.filterVisible()

        if (oldVisibleTransactions != newVisibleTransactions) {
            val oldTopTransaction = oldVisibleTransactions.lastOrNull()
            val newTopTransaction = newVisibleTransactions.lastOrNull()

            // check if we should animate the top transactions
            val replacingTopTransactions = newTopTransaction != null && (oldTopTransaction == null
                    || oldTopTransaction.controller != newTopTransaction.controller)

            // Remove all visible controllers that were previously on the backstack
            // from top to bottom
            oldVisibleTransactions
                .dropLast(if (replacingTopTransactions) 1 else 0)
                .reversed()
                .filterNot { old -> newVisibleTransactions.any { it.controller == old.controller } }
                .forEach { transaction ->
                    toBeInvisibleControllers.add(transaction.controller)

                    ControllerChangeManager.cancelChange(transaction.controller.instanceId)
                    val localHandler = handler?.copy()
                        ?: transaction.popChangeHandler?.copy()
                        ?: DefaultChangeHandler()

                    performControllerChange(
                        transaction,
                        null,
                        isPush,
                        localHandler,
                        true,
                        -1
                    )
                }

            // Add any new controllers to the backstack from bottom to top
            newVisibleTransactions
                .dropLast(if (replacingTopTransactions) 1 else 0)
                .filterNot { new -> oldVisibleTransactions.any { it.controller == new.controller } }
                .forEachIndexed { i, transaction ->
                    val localHandler = handler?.copy() ?: transaction.pushChangeHandler
                    val from = newVisibleTransactions.getOrNull(i - 1)
                    performControllerChange(
                        from,
                        transaction,
                        true,
                        localHandler,
                        false,
                        getToIndex(transaction, from, true)
                    )
                }

            // Replace the old visible top with the new one
            if (replacingTopTransactions) {
                oldTopTransaction?.let { toBeInvisibleControllers.add(it.controller) }

                val localHandler = handler?.copy()
                    ?: (if (isPush) newTopTransaction?.pushChangeHandler?.copy()
                    else oldTopTransaction?.popChangeHandler?.copy())
                    ?: DefaultChangeHandler()

                val forceRemoveFromView =
                    oldTopTransaction != null && !newVisibleTransactions.contains(oldTopTransaction)

                performControllerChange(
                    oldTopTransaction,
                    newTopTransaction,
                    isPush,
                    localHandler,
                    forceRemoveFromView,
                    getToIndex(newTopTransaction, oldTopTransaction, isPush)
                )
            }
        }

        // destroy all invisible transactions here
        destroyedInvisibleTransactions.reversed().forEach {
            it.controller.destroyView(false)
            it.controller.destroy()
        }
    }

    private fun rebind() {
        if (container == null) return

        _backstack
            .filterVisible()
            .forEach {
                performControllerChange(
                    null, it, true,
                    DefaultChangeHandler(false),
                    false,
                    getToIndex(it, null, true)
                )
            }
    }

    private fun List<Transaction>.filterVisible(): List<Transaction> =
        takeLastUntil {
            it.pushChangeHandler != null
                    && !it.pushChangeHandler!!.removesFromViewOnPush
        }

    private fun getToIndex(
        to: Transaction?,
        from: Transaction?,
        isPush: Boolean
    ): Int {
        val container = container ?: return -1
        if (to == null) return -1
        return if (isPush || from == null) {
            if (container.childCount == 0) return -1
            val backstackIndex =
                _backstack.indexOfFirst { it.controller.view == to.controller.view }
            (0 until container.childCount)
                .map(container::getChildAt)
                .indexOfFirst { v ->
                    _backstack.indexOfFirst { it.controller.view == v } > backstackIndex
                }
        } else {
            val currentToIndex = container.indexOfChild(to.controller.view)
            val currentFromIndex = container.indexOfChild(from.controller.view)

            if (currentToIndex == -1 || currentToIndex > currentFromIndex) {
                container.indexOfChild(from.controller.view)
            } else {
                currentToIndex
            }
        }
    }


    companion object {
        private const val KEY_BACKSTACK = "StackRouter.backstack"
        private const val KEY_POPS_LAST_VIEW = "StackRouter.popsLastView"
    }
}

val StackRouter.backstackSize: Int get() = backstack.size

val StackRouter.hasRoot: Boolean get() = backstackSize > 0

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun StackRouter.setRoot(transaction: Transaction, handler: ChangeHandler? = null) {
    // todo check if we should always use isPush=true
    setBackstack(listOf(transaction), true, handler ?: transaction.pushChangeHandler)
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun StackRouter.push(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(transaction)
    setBackstack(newBackstack, true, handler)
}

/**
 * Replaces this Router's top [Controller] with the [Controller] of the [transaction]
 */
fun StackRouter.replaceTop(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    val from = newBackstack.lastOrNull()
    if (from != null) {
        newBackstack.removeAt(newBackstack.lastIndex)
    }
    newBackstack.add(transaction)
    setBackstack(newBackstack, true, handler)
}

/**
 * Pops the passed [Controller] from the backstack
 */
fun StackRouter.pop(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { pop(it, handler) }
}

/**
 * Pops the passed [transaction] from the backstack
 */
fun StackRouter.pop(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val oldBackstack = backstack
    val newBackstack = oldBackstack.toMutableList()
    newBackstack.removeAll { it == transaction }
    setBackstack(newBackstack, false, handler)
}

/**
 * Pops the top [Controller] from the backstack
 */
fun StackRouter.popTop(handler: ChangeHandler? = null) {
    val transaction = backstack.lastOrNull()
        ?: error("Trying to pop the current controller when there are none on the backstack.")
    pop(transaction, handler)
}

/**
 * Pops all [Controller] until only the root is left
 */
fun StackRouter.popToRoot(handler: ChangeHandler? = null) {
    backstack.firstOrNull()?.let { popTo(it, handler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun StackRouter.popToTag(tag: String, handler: ChangeHandler? = null) {
    backstack.firstOrNull { it.tag == tag }
        ?.let { popTo(it, handler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun StackRouter.popTo(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { popTo(it, handler) }
}

/***
 * Pops all [Controller]s until the [transaction] is at the top
 */
fun StackRouter.popTo(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.dropLastWhile { it != transaction }
    setBackstack(newBackstack, false, handler)
}