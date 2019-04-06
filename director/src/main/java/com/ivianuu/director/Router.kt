package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.closeable.Closeable
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.stdlibx.firstNotNullResultOrNull
import com.ivianuu.stdlibx.safeAs
import com.ivianuu.stdlibx.takeLastUntil

/**
 * Handles the backstack and delegates the host lifecycle to it's [Controller]s
 */
class Router internal constructor(
    containerId: Int,
    tag: String? = null,
    val routerManager: RouterManager
) {

    /**
     * The current backstack
     */
    val backstack: List<Controller> get() = _backstack
    private val _backstack = mutableListOf<Controller>()

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = false

    /**
     * The tag of this router
     */
    var tag: String? = tag
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
        mutableListOf<ListenerEntry<RouterListener>>()
    private val controllerListeners =
        mutableListOf<ListenerEntry<ControllerListener>>()

    private val runningHandlers =
        mutableMapOf<Controller, ChangeHandler>()

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ChangeHandler]
     */
    fun setBackstack(
        newBackstack: List<Controller>,
        isPush: Boolean,
        handler: ChangeHandler? = null
    ) {
        if (newBackstack == _backstack) return

        // Swap around transaction indices to ensure they don't get thrown out of order by the
        // developer rearranging the backstack at runtime.
        val indices = newBackstack
            .onEach {
                if (it.transactionIndex == -1) {
                    it.transactionIndex = routerManager.transactionIndexer.nextIndex()
                }
            }
            .map(Controller::transactionIndex)
            .sorted()

        newBackstack.forEachIndexed { i, controller ->
            controller.transactionIndex = indices[i]
        }

        check(newBackstack.size == newBackstack.distinct().size) {
            "Trying to push the same controller to the backstack more than once."
        }
        newBackstack.forEach {
            check(it.state != DESTROYED) {
                "Trying to push a controller that has already been destroyed ${it.javaClass.simpleName}"
            }
        }

        val oldBackstack = _backstack.toList()
        val oldVisibleControllers = oldBackstack.filterVisible()

        _backstack.clear()
        _backstack.addAll(newBackstack)

        // find destroyed controllers
        val destroyedControllers = oldBackstack
            .filter { old -> newBackstack.none { it == old } }

        val destroyedInvisibleControllers = destroyedControllers
            .filterNot(Controller::isAttached)

        // Ensure all new controllers have a valid router set
        newBackstack.forEach(this::moveControllerToCorrectState)

        val newVisibleControllers = newBackstack.filterVisible()

        if (oldVisibleControllers != newVisibleControllers) {
            val oldTopController = oldVisibleControllers.lastOrNull()
            val newTopController = newVisibleControllers.lastOrNull()

            // check if we should animate the top controllers
            val replacingTopControllers = newTopController != null && (oldTopController == null
                    || oldTopController != newTopController)

            // Remove all visible controllers that were previously on the backstack
            // from top to bottom
            oldVisibleControllers
                .dropLast(if (replacingTopControllers) 1 else 0)
                .reversed()
                .filterNot(newVisibleControllers::contains)
                .forEach { controller ->
                    cancelChange(controller)

                    val localHandler = handler?.copy()
                        ?: controller.popChangeHandler?.copy()
                        ?: DefaultChangeHandler()

                    performControllerChange(
                        from = controller,
                        to = null,
                        isPush = isPush,
                        handler = localHandler,
                        forceRemoveFromViewOnPush = true,
                        onFromDetached = {
                            controller.detach()

                            val willBeDestroyed = !newBackstack.contains(controller)

                            controller.destroyView(!willBeDestroyed)

                            if (willBeDestroyed) {
                                controller.destroy()
                            }
                        }
                    )
                }

            // Add any new controllers to the backstack from bottom to top
            newVisibleControllers
                .dropLast(if (replacingTopControllers) 1 else 0)
                .filterNot(oldVisibleControllers::contains)
                .forEachIndexed { i, controller ->
                    val localHandler = handler?.copy() ?: controller.pushChangeHandler
                    val prevController = newVisibleControllers.getOrNull(i - 1)
                    performControllerChange(
                        from = prevController,
                        to = controller,
                        isPush = true,
                        handler = localHandler,
                        forceRemoveFromViewOnPush = false,
                        onToAttached = {
                            if (routerManager.isStarted) {
                                controller.attach()
                            }
                        }
                    )
                }

            // Replace the old visible top with the new one
            if (replacingTopControllers) {
                val localHandler = handler?.copy()
                    ?: (if (isPush) newTopController?.pushChangeHandler?.copy()
                    else oldTopController?.popChangeHandler?.copy())
                    ?: DefaultChangeHandler()

                val willBeVisible =
                    oldTopController != null && newVisibleControllers.contains(oldTopController)

                performControllerChange(
                    from = oldTopController,
                    to = newTopController,
                    isPush = isPush,
                    handler = localHandler,
                    forceRemoveFromViewOnPush = !willBeVisible,
                    onToAttached = {
                        if (routerManager.isStarted) {
                            newTopController?.attach()
                        }
                    },
                    onFromDetached = {
                        if (oldTopController != null) {
                            if (!willBeVisible) {
                                oldTopController.detach()
                            }

                            val willBeDestroyed = !newBackstack.contains(oldTopController)

                            if (!willBeVisible) {
                                oldTopController.destroyView(!willBeDestroyed)
                            }

                            if (willBeDestroyed) {
                                oldTopController.destroy()
                            }
                        }
                    }
                )
            }
        }

        // destroy all invisible transactions here
        destroyedInvisibleControllers.reversed().forEach {
            it.destroyView(false)
            it.destroy()
        }
    }

    /**
     * Let the current controller handles back click or pops the top controller if possible
     * Returns whether or not the back click was handled
     */
    fun handleBack(): Boolean {
        val topController = backstack.lastOrNull()

        return if (topController != null) {
            if (topController.handleBack()) {
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
    fun addListener(listener: RouterListener, recursive: Boolean = false): Closeable {
        listeners.add(ListenerEntry(listener, recursive))
        return Closeable { removeListener(listener) }
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeListener(listener: RouterListener) {
        listeners.removeAll { it.listener == listener }
    }

    /**
     * Adds the [listener] to all controllers
     */
    fun addControllerListener(listener: ControllerListener, recursive: Boolean = false): Closeable {
        controllerListeners.add(ListenerEntry(listener, recursive))
        return Closeable { removeControllerListener(listener) }
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeControllerListener(listener: ControllerListener) {
        controllerListeners.removeAll { it.listener == listener }
    }

    internal fun getListeners(recursiveOnly: Boolean = false): List<RouterListener> {
        return listeners
            .filter { !recursiveOnly || it.recursive }
            .map(ListenerEntry<RouterListener>::listener) +
                (routerManager.host.safeAs<Controller>()?.router?.getListeners(true)
                    ?: emptyList())
    }

    internal fun getControllerListeners(recursiveOnly: Boolean = false): List<ControllerListener> {
        return controllerListeners
            .filter { !recursiveOnly || it.recursive }
            .map(ListenerEntry<ControllerListener>::listener) +
                (routerManager.host.safeAs<Controller>()?.router?.getControllerListeners(true)
                    ?: emptyList())
    }

    /**
     * Sets the container of this router
     */
    fun setContainer(container: ViewGroup) {
        require(container.id == containerId) {
            "container id of the container must match the container id of this router"
        }

        if (this.container != container) {
            removeContainer()
            this.container = container
            rebind()
        }
    }

    /**
     * Removes the current container if set
     */
    fun removeContainer() {
        if (container == null) return
        prepareForContainerRemoval()
        _backstack.reversed().forEach { it.destroyView(false) }
        container = null
    }

    internal fun onStart() {
        // attach visible controllers
        _backstack
            .filterVisible()
            .forEach(Controller::attach)
    }

    internal fun onStop() {
        prepareForContainerRemoval()
        _backstack
            .filterVisible()
            .reversed()
            .forEach(Controller::detach)
    }

    internal fun onDestroy() {
        _backstack.reversed().forEach(Controller::destroy)
    }

    /**
     * Saves the state of this router
     */
    fun saveInstanceState(): Bundle {
        prepareForContainerRemoval()

        return Bundle().apply {
            putInt(KEY_CONTAINER_ID, containerId)
            putString(KEY_TAG, tag)
            val backstack = _backstack.map(Controller::saveInstanceState)
            putParcelableArrayList(KEY_BACKSTACK, ArrayList(backstack))
            putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
        }
    }

    /**
     * Restores the previously saved state state
     */
    fun restoreInstanceState(savedInstanceState: Bundle) {
        _backstack.clear()
        _backstack.addAll(
            savedInstanceState.getParcelableArrayList<Bundle>(KEY_BACKSTACK)!!
                .map { Controller.fromBundle(it, routerManager.controllerFactory) }
        )

        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        _backstack.forEach(this::moveControllerToCorrectState)
        rebind()
    }

    private fun performControllerChange(
        from: Controller?,
        to: Controller?,
        isPush: Boolean,
        handler: ChangeHandler? = null,
        forceRemoveFromViewOnPush: Boolean,
        onToAttached: (() -> Unit)? = null,
        onFromDetached: (() -> Unit)? = null,
        onComplete: (() -> Unit?)? = null
    ) {
        val container = container ?: return
        val listeners = getListeners()

        val handlerToUse = when {
            handler == null -> DefaultChangeHandler()
            handler.hasBeenUsed -> handler.copy()
            else -> handler
        }
        handlerToUse.hasBeenUsed = true

        from?.let(this::cancelChange)
        to?.let { runningHandlers[it] = handlerToUse }

        listeners.forEach { it.onChangeStarted(this, to, from, isPush, container, handlerToUse) }

        val toView = to?.view ?: to?.createView(container)
        val fromView = from?.view

        val toIndex = getToIndex(to, from, isPush)

        val callback = object : ChangeHandler.Callback {
            override fun addToView() {
                val addingToView = toView != null && toView.parent == null
                val movingToView = toView != null && container.indexOfChild(toView) != toIndex
                if (addingToView) {
                    container.addView(toView, toIndex)
                    toViewAdded()
                } else if (movingToView) {
                    container.moveView(toView!!, toIndex)
                    toViewAdded()
                }
            }

            override fun toViewAdded() {
                onToAttached?.invoke()
            }

            override fun removeFromView() {
                if (fromView != null && (!isPush || handlerToUse.removesFromViewOnPush
                            || forceRemoveFromViewOnPush)
                ) {
                    container.removeView(fromView)
                    fromViewRemoved()
                }
            }

            override fun fromViewRemoved() {
                onFromDetached?.invoke()
            }

            override fun onChangeCompleted() {
                to?.let(runningHandlers::remove)
                onComplete?.invoke()
                listeners.forEach {
                    it.onChangeCompleted(
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
            forceRemoveFromViewOnPush
        )

        handlerToUse.performChange(changeData)
    }

    private fun moveControllerToCorrectState(controller: Controller) {
        controller.create(this)

        if (routerManager.isStarted && controller.view?.windowToken != null) {
            controller.attach()
        }

        if (routerManager.isDestroyed) {
            controller.destroy()
        }
    }

    private fun prepareForContainerRemoval() {
        _backstack.reversed().forEach(this::cancelChange)
    }

    private fun List<Controller>.filterVisible(): List<Controller> =
        takeLastUntil {
            it.pushChangeHandler != null
                    && !it.pushChangeHandler!!.removesFromViewOnPush
        }

    private fun rebind() {
        if (container == null) return

        _backstack
            .filterVisible()
            .forEach {
                performControllerChange(
                    from = null,
                    to = it,
                    isPush = true,
                    handler = DefaultChangeHandler(false),
                    forceRemoveFromViewOnPush = false,
                    onToAttached = {
                        if (routerManager.isStarted) {
                            it.attach()
                        }
                    }
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
            val backstackIndex = backstack.indexOfFirst { it == to }
            (0 until container.childCount)
                .map(container::getChildAt)
                .indexOfFirst { v ->
                    backstack.indexOfFirst { it.view == v } > backstackIndex
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

fun Router.getControllerByTagOrNull(tag: String): Controller? =
    backstack.firstNotNullResultOrNull {
        if (it.tag == tag) {
            it
        } else {
            it.childRouterManager
                .getControllerByTagOrNull(tag)
        }
    }

fun Router.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun Router.getControllerByInstanceIdOrNull(instanceId: String): Controller? =
    backstack.firstNotNullResultOrNull {
        if (it.instanceId == instanceId) {
            it
        } else {
            it.childRouterManager
                .getControllerByInstanceIdOrNull(instanceId)
        }
    }

fun Router.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun Router.setRoot(controller: Controller, handler: ChangeHandler? = null) {
    // todo check if we should always use isPush=true
    setBackstack(listOf(controller), true, handler ?: controller.pushChangeHandler)
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.push(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(controller)
    setBackstack(newBackstack, true, handler)
}

/**
 * Replaces this Router's top [Controller] with the [controller]
 */
fun Router.replaceTop(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    val from = newBackstack.lastOrNull()
    if (from != null) {
        newBackstack.removeAt(newBackstack.lastIndex)
    }
    newBackstack.add(controller)
    setBackstack(newBackstack, true, handler)
}

/**
 * Pops the passed [controller] from the backstack
 */
fun Router.pop(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    val oldBackstack = backstack
    val newBackstack = oldBackstack.toMutableList()
    newBackstack.remove(controller)
    setBackstack(newBackstack, false, handler)
}

/**
 * Pops the top [Controller] from the backstack
 */
fun Router.popTop(handler: ChangeHandler? = null) {
    val controller = backstack.lastOrNull()
        ?: error("Trying to pop the current controller when there are none on the backstack.")
    pop(controller, handler)
}

/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(handler: ChangeHandler? = null) {
    backstack.firstOrNull()?.let { popTo(it, handler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, handler: ChangeHandler? = null) {
    backstack.firstOrNull { it.tag == tag }
        ?.let { popTo(it, handler) }
}

/***
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popTo(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.dropLastWhile { it != controller }
    setBackstack(newBackstack, false, handler)
}