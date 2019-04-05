package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.stdlibx.takeLastUntil

/**
 * Handles the backstack and delegates the host lifecycle to it's [Controller]s
 */
class Router(
    id: Int,
    tag: String?,
    routerManager: RouterManager
) : ViewGroupRouter(id, tag, routerManager) {

    override val controllers: List<Controller>
        get() = backstack

    /**
     * The current backstack
     */
    val backstack: List<Controller> get() = _backstack
    private val _backstack = mutableListOf<Controller>()

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = false

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

    override fun onContainerSet(container: ViewGroup) {
        super.onContainerSet(container)
        rebind()
    }

    override fun onContainerRemoved(container: ViewGroup) {
        super.onContainerRemoved(container)
        _backstack.reversed().forEach { it.destroyView(false) }
    }

    override fun onStart() {
        super.onStart()
        _backstack.forEach(Controller::attach)
    }

    override fun onStop() {
        super.onStop()
        prepareForContainerRemoval()
        _backstack.reversed().forEach(Controller::detach)
    }

    override fun onDestroy() {
        super.onDestroy()
        _backstack.reversed().forEach(Controller::destroy)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        _backstack.clear()
        _backstack.addAll(
            savedInstanceState.getParcelableArrayList<Bundle>(KEY_BACKSTACK)!!
                .map { Controller.fromBundle(it, routerManager.controllerFactory) }
        )

        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        _backstack.forEach(this::moveControllerToCorrectState)
        rebind()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        prepareForContainerRemoval()

        val backstack = _backstack.map(Controller::saveInstanceState)
        outState.putParcelableArrayList(KEY_BACKSTACK, ArrayList(backstack))
        outState.putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
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

        if (routerManager.isStarted) {
            controller.attach()
        }

        if (routerManager.isDestroyed) {
            controller.destroy()
        }
    }

    private fun prepareForContainerRemoval() {
        _backstack.reversed().forEach {
            ControllerChangeManager.cancelChange(it.instanceId)
        }
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

    companion object {
        private const val KEY_BACKSTACK = "Router.backstack"
        private const val KEY_POPS_LAST_VIEW = "Router.popsLastView"
    }
}

val Router.backstackSize: Int get() = backstack.size

val Router.hasRoot: Boolean get() = backstackSize > 0

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