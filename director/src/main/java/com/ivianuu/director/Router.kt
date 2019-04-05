package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.closeable.Closeable
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.internal.ControllerChangeManager
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

    internal val internalControllerListener = ControllerListener(
        postDetach = { controller, _ ->
            if (destroyingControllers.contains(controller)) {
                controller.destroyView(false)
            } else if (toBeInvisibleControllers.contains(controller)) {
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

        val (destroyedVisibleControllers, destroyedInvisibleControllers) =
            destroyedControllers
                .partition(Controller::isAttached)

        destroyingControllers.addAll(destroyedVisibleControllers)

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
                    toBeInvisibleControllers.add(controller)

                    ControllerChangeManager.cancelChange(controller.instanceId)
                    val localHandler = handler?.copy()
                        ?: controller.popChangeHandler?.copy()
                        ?: DefaultChangeHandler()

                    performControllerChange(
                        controller,
                        null,
                        isPush,
                        localHandler,
                        true
                    )
                }

            // Add any new controllers to the backstack from bottom to top
            newVisibleControllers
                .dropLast(if (replacingTopControllers) 1 else 0)
                .filterNot(oldVisibleControllers::contains)
                .forEachIndexed { i, controller ->
                    val localHandler = handler?.copy() ?: controller.pushChangeHandler
                    performControllerChange(
                        newVisibleControllers.getOrNull(i - 1),
                        controller,
                        true,
                        localHandler,
                        false
                    )
                }

            // Replace the old visible top with the new one
            if (replacingTopControllers) {
                oldTopController?.let(toBeInvisibleControllers::add)

                val localHandler = handler?.copy()
                    ?: (if (isPush) newTopController?.pushChangeHandler?.copy()
                    else oldTopController?.popChangeHandler?.copy())
                    ?: DefaultChangeHandler()

                val forceRemoveFromView =
                    oldTopController != null && !newVisibleControllers.contains(oldTopController)

                performControllerChange(
                    oldTopController,
                    newTopController,
                    isPush,
                    localHandler,
                    forceRemoveFromView
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

    internal fun hostStarted() {
        _backstack.forEach(Controller::attach)
    }

    internal fun hostStopped() {
        prepareForContainerRemoval()
        _backstack.reversed().forEach(Controller::detach)
    }

    internal fun hostDestroyed() {
        _backstack.reversed().forEach(Controller::destroy)
        removeContainer()
    }

    private fun prepareForContainerRemoval() {
        _backstack.reversed().forEach {
            ControllerChangeManager.cancelChange(it.instanceId)
        }
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
        forceRemoveFromViewOnPush: Boolean
    ) {
        val container = container ?: return
        ControllerChangeManager.executeChange(
            this,
            from,
            to,
            isPush,
            container,
            handler,
            forceRemoveFromViewOnPush,
            getListeners()
        )
    }

    private fun moveControllerToCorrectState(controller: Controller) {
        controller.create(this)

        if (routerManager.hostStarted) {
            controller.attach()
        }

        if (routerManager.hostDestroyed) {
            controller.destroy()
        }
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
                    null, it, true,
                    DefaultChangeHandler(false),
                    false
                )
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