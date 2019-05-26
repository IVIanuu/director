package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.internal.moveView

/**
 * Handles the backstack and delegates the host lifecycle to it's [Controller]s
 */
class Router internal constructor(
    containerId: Int,
    tag: String? = null,
    val routerManager: RouterManager
) {

    /**
     * The tag of this router
     */
    var tag: String? = tag
        private set

    /**
     * The current backstack
     */
    val backstack: List<RouterTransaction> get() = _backstack
    private val _backstack = mutableListOf<RouterTransaction>()

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = false

    /**
     * Will be used to instantiate controllers after config changes or process death
     */
    var controllerFactory: ControllerFactory = routerManager.controllerFactory

    /**
     * Whether or not this router is started
     */
    var isStarted = false
        private set

    var isBeingDestroyed = false
        private set

    /**
     * Whether or not this router has been destroyed
     */
    var isDestroyed = false
        private set

    /**
     * The container id of this router
     */
    var containerId: Int = containerId
        private set

    /**
     * The container of this router
     */
    var container: ViewGroup? = null
        private set

    private val listeners =
        mutableListOf<ListenerEntry<ControllerChangeListener>>()
    private val controllerListeners =
        mutableListOf<ListenerEntry<ControllerLifecycleListener>>()

    private val runningHandlers =
        mutableMapOf<Controller, ControllerChangeHandler>()

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ControllerChangeHandler]
     */
    fun setBackstack(
        newBackstack: List<RouterTransaction>,
        isPush: Boolean,
        handler: ControllerChangeHandler? = null
    ) {
        if (isDestroyed) return

        if (newBackstack == _backstack) return

        // do not allow pushing the same controller twice
        newBackstack
            .groupBy { it }
            .forEach {
                check(it.value.size == 1) {
                    "Trying to push the same controller to the backstack more than once. $it"
                }
            }

        // do not allow pushing destroyed controllers
        newBackstack.forEach {
            check(it.controller.state != DESTROYED) {
                "Trying to push a controller that has already been destroyed $it"
            }
        }

        // do not allow pushing controllers which are already attached to another router
        newBackstack.forEach {
            check(!it.isAttachedToRouter || it.controller.router == this) {
                "Trying to push a controller which is attached to another router $it"
            }
        }

        // Swap around transaction indices to ensure they don't get thrown out of order by the
        // developer rearranging the backstack at runtime.
        val indices = newBackstack
            .onEach { it.ensureValidIndex(routerManager.transactionIndexer) }
            .map { it.transactionIndex }
            .sorted()

        newBackstack.forEachIndexed { i, controller ->
            controller.transactionIndex = indices[i]
        }

        val oldBackstack = _backstack.toList()
        val oldVisibleTransactions = oldBackstack.filterVisible()

        _backstack.clear()
        _backstack.addAll(newBackstack)

        // find destroyed controllers
        val destroyedTransactions = oldBackstack
            .filter { old -> newBackstack.none { it == old } }

        destroyedTransactions.forEach { it.controller.willBeDestroyed() }

        val destroyedInvisibleTransactions = destroyedTransactions
            .filterNot { it.controller.isViewCreated }

        // Ensure all new controllers have a valid router set
        newBackstack.forEach { moveControllerToCorrectState(it.controller) }

        val newVisibleTransactions = newBackstack.filterVisible()

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
                .filterNot { newVisibleTransactions.contains(it) }
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

            // Add any new controllers to the backstack from bottom to top
            newVisibleTransactions
                .dropLast(if (replacingTopControllers) 1 else 0)
                .filterNot { oldVisibleTransactions.contains(it) }
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

                performControllerChange(
                    from = oldTopTransaction?.controller,
                    to = newTopTransaction?.controller,
                    isPush = isPush,
                    handler = localHandler,
                    forceRemoveFromView = oldTopTransaction?.controller?.isBeingDestroyed ?: false
                )
            }
        }

        // destroy all invisible transactions here
        destroyedInvisibleTransactions.reversed().forEach { it.controller.destroy() }
    }

    /**
     * Let the current controller handles back click or pops the top controller if possible
     * Returns whether or not the back click was handled
     */
    fun handleBack(): Boolean {
        if (isDestroyed) return false
        val topTransaction = backstack.lastOrNull()

        return if (topTransaction != null) {
            if (topTransaction.controller.handleBack()) {
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
     * Notifies the [listener] on controller changes
     */
    fun addChangeListener(listener: ControllerChangeListener, recursive: Boolean = false) {
        listeners.add(ListenerEntry(listener, recursive))
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeChangeListener(listener: ControllerChangeListener) {
        listeners.removeAll { it.listener == listener }
    }

    /**
     * Adds the [listener] to all controllers
     */
    fun addControllerLifecycleListener(
        listener: ControllerLifecycleListener,
        recursive: Boolean = false
    ) {
        controllerListeners.add(ListenerEntry(listener, recursive))
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeControllerLifecycleListener(listener: ControllerLifecycleListener) {
        controllerListeners.removeAll { it.listener == listener }
    }

    internal fun getChangeListeners(recursiveOnly: Boolean = false): List<ControllerChangeListener> {
        return listeners
            .filter { !recursiveOnly || it.recursive }
            .map { it.listener } + (routerManager.parent?.router?.getChangeListeners(true)
            ?: emptyList())
    }

    internal fun getControllerLifecycleListeners(recursiveOnly: Boolean = false): List<ControllerLifecycleListener> {
        return controllerListeners
            .filter { !recursiveOnly || it.recursive }
            .map { it.listener } + (routerManager.parent?.router?.getControllerLifecycleListeners(
            true
        ) ?: emptyList())
    }

    /**
     * Sets the container of this router
     */
    fun setContainer(container: ViewGroup) {
        require(container.id == containerId) {
            "Container id of the container must match the container id of this router $containerId"
        }

        this.container = container
        rebind()
    }

    /**
     * Removes the current container if set
     */
    fun removeContainer() {
        endAllChanges()
        _backstack.reversed()
            .forEach {
                if (it.controller.isAttached) {
                    it.controller.detach()
                }

                if (it.controller.isViewCreated) {
                    if (!it.controller.retainView || it.controller.isBeingDestroyed) {
                        it.controller.destroyView()
                    } else if (it.controller.retainView) {
                        it.controller.removeChildRootView()
                        (it.controller.view!!.parent as ViewGroup)
                            .removeView(it.controller.view)
                    }
                }
            }
        container?.removeAllViews()
        container = null
    }

    internal fun start() {
        isStarted = true
        // attach visible controllers
        _backstack
            .filterVisible()
            .filter { it.controller.view?.parent != null }
            .filterNot { it.controller.isAttached }
            .forEach { it.controller.attach() }
    }

    internal fun stop() {
        isStarted = false

        endAllChanges()
        _backstack
            .filter { it.controller.isAttached }
            .reversed()
            .forEach { it.controller.detach() }
    }

    internal fun willBeDestroyed() {
        isBeingDestroyed = true

        _backstack.reversed()
            .forEach { it.controller.willBeDestroyed() }
    }

    internal fun destroy() {
        isDestroyed = true

        _backstack.reversed()
            .forEach { it.controller.destroy() }
    }

    /**
     * Saves the state of this router
     */
    fun saveInstanceState(): Bundle {
        endAllChanges()

        return Bundle().apply {
            putInt(KEY_CONTAINER_ID, containerId)
            putString(KEY_TAG, tag)
            val backstack = _backstack.map { it.toBundle() }
            putParcelableArrayList(KEY_BACKSTACK, ArrayList(backstack))
            putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
        }
    }

    /**
     * Restores the previously saved state state
     */
    fun restoreInstanceState(savedInstanceState: Bundle) {
        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        val newBackstack =
            savedInstanceState.getParcelableArrayList<Bundle>(KEY_BACKSTACK)!!
                .map { RouterTransaction.fromBundle(it, controllerFactory) }

        setBackstack(newBackstack, true)
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

        val toChangeType =
            if (isPush) ControllerChangeType.PUSH_ENTER else ControllerChangeType.POP_ENTER

        val fromChangeType =
            if (isPush) ControllerChangeType.PUSH_EXIT else ControllerChangeType.POP_EXIT

        val toView = to?.view?.also { to.setChildRootView() } ?: to?.createView(container)
        to?.changeStarted(from, handlerToUse, toChangeType)

        val fromView = from?.view
        from?.changeStarted(to, handlerToUse, fromChangeType)

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

                if (!from.retainView
                    || from.isBeingDestroyed
                ) {
                    from.destroyView()
                } else if (from.retainView) {
                    from.removeChildRootView()
                }

                if (from.isBeingDestroyed) {
                    from.destroy()
                }
            }

            override fun changeCompleted() {
                from?.changeEnded(to, handlerToUse, fromChangeType)

                if (to != null) {
                    runningHandlers.remove(to)
                    to.changeEnded(from, handlerToUse, toChangeType)
                }

                listeners.forEach {
                    it.onChangeEnded(
                        this@Router,
                        to,
                        from,
                        isPush,
                        container,
                        handlerToUse
                    )
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
        if (!controller.isCreated) {
            controller.create(this)
        }

        if (isStarted
            && controller.view?.parent != null
            && !controller.isAttached
        ) {
            controller.attach()
        }

        if (isBeingDestroyed) {
            controller.willBeDestroyed()
        }

        if (isDestroyed && !controller.isDestroyed) {
            controller.destroy()
        }
    }

    private fun endAllChanges() {
        _backstack.reversed().forEach { cancelChange(it.controller) }
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

        _backstack
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
            val backstackIndex = backstack.indexOfFirst { it.controller == to }
            (0 until container.childCount)
                .map { container.getChildAt(it) }
                .indexOfFirst { v ->
                    backstack.indexOfFirst { it.controller.view == v } > backstackIndex
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

    private data class ListenerEntry<T>(
        val listener: T,
        val recursive: Boolean
    )

    companion object {
        private const val KEY_BACKSTACK = "Router.backstack"
        private const val KEY_CONTAINER_ID = "Router.containerId"
        private const val KEY_POPS_LAST_VIEW = "Router.popsLastView"
        private const val KEY_TAG = "Router.tag"

        fun fromBundle(
            bundle: Bundle,
            routerManager: RouterManager
        ): Router = Router(
            bundle.getInt(KEY_CONTAINER_ID),
            bundle.getString(KEY_TAG),
            routerManager
        )
    }
}

val Router.hasContainer: Boolean get() = container != null

val Router.backstackSize: Int get() = backstack.size

val Router.hasRoot: Boolean get() = backstackSize > 0

fun Router.getControllerByTagOrNull(tag: String): Controller? {
    for (transaction in backstack) {
        if (transaction.tag == tag) {
            return transaction.controller
        }

        transaction.controller.childRouterManager.getControllerByTagOrNull(tag)
            ?.let { return@getControllerByTagOrNull it }
    }

    return null
}

fun Router.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun Router.getControllerByInstanceIdOrNull(instanceId: String): Controller? {
    for (transaction in backstack) {
        if (transaction.controller.instanceId == instanceId) {
            return transaction.controller
        }

        transaction.controller.childRouterManager.getControllerByInstanceIdOrNull(instanceId)
            ?.let { return@getControllerByInstanceIdOrNull it }
    }

    return null
}

fun Router.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun Router.setRoot(transaction: RouterTransaction, handler: ControllerChangeHandler? = null) {
    // todo check if we should always use isPush=true
    setBackstack(listOf(transaction), true, handler ?: transaction.pushChangeHandler)
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.push(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(transaction)
    setBackstack(newBackstack, true, handler)
}

/**
 * Replaces this Router's top [Controller] with the [controller]
 */
fun Router.replaceTop(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.lastOrNull()?.let { newBackstack.remove(it) }
    newBackstack.add(transaction)
    setBackstack(newBackstack, true, handler)
}

/**
 * Pops the passed [transaction] from the backstack
 */
fun Router.popController(
    controller: Controller,
    handler: ControllerChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { popTransaction(it, handler) }
}

/**
 * Pops the passed [transaction] from the backstack
 */
fun Router.popTransaction(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.remove(transaction)
    setBackstack(newBackstack, false, handler)
}

/**
 * Pops the top [Controller] from the backstack
 */
fun Router.popTop(handler: ControllerChangeHandler? = null) {
    val controller = backstack.lastOrNull()
        ?: error("Trying to pop the current controller when there are none on the backstack.")
    popTransaction(controller, handler)
}

/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(handler: ControllerChangeHandler? = null) {
    backstack.firstOrNull()?.let { popToTransaction(it, handler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, handler: ControllerChangeHandler? = null) {
    backstack.firstOrNull { it.tag == tag }?.let { popToTransaction(it, handler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popToController(
    controller: Controller,
    handler: ControllerChangeHandler? = null
) {
    backstack
        .firstOrNull { it.controller == controller }
        ?.let { popToTransaction(it, handler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popToTransaction(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.dropLastWhile { it != transaction }
    setBackstack(newBackstack, false, handler)
}

/**
 * Clears out the backstack
 */
fun Router.clear(handler: ControllerChangeHandler? = null) {
    setBackstack(emptyList(), false, handler)
}