package com.ivianuu.director.androidx.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerState
import com.ivianuu.director.controllerLifecycleListener
import com.ivianuu.director.doOnPostUnbindView
import com.ivianuu.director.hasView
import com.ivianuu.director.isAtLeast

/**
 * A [LifecycleOwner] for [Controller]s views
 */
class ControllerViewLifecycleOwner(controller: Controller) : LifecycleOwner {

    private var lifecycleRegistry: LifecycleRegistry? = null

    private val listener = controllerLifecycleListener {
        postInflateView { _, _, _ ->
            lifecycleRegistry = LifecycleRegistry(this@ControllerViewLifecycleOwner)
        }
        postBindView { _, _, _ ->
            lifecycleRegistry?.handleLifecycleEvent(ON_CREATE)
            lifecycleRegistry?.handleLifecycleEvent(ON_START)
        }
        postAttach { _, _ ->
            lifecycleRegistry?.handleLifecycleEvent(ON_RESUME)
        }
        preDetach { _, _ ->
            lifecycleRegistry?.handleLifecycleEvent(ON_PAUSE)
        }
        preUnbindView { _, _ ->
            lifecycleRegistry?.handleLifecycleEvent(ON_STOP)
            lifecycleRegistry?.handleLifecycleEvent(ON_DESTROY)
        }
        postUnbindView { lifecycleRegistry = null }
    }

    init {
        controller.addLifecycleListener(listener)

        if (controller.state.isAtLeast(ControllerState.VIEW_BOUND)) {
            listener.postInflateView(controller, controller.view!!, null)
            listener.postBindView(controller, controller.view!!, null)
        }

        if (controller.state.isAtLeast(ControllerState.ATTACHED)) {
            listener.postAttach(controller, controller.view!!)
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
        ?: error("only accessible between onBindView and onUnbindView")
}

private val viewLifecycleOwnersByController = mutableMapOf<Controller, LifecycleOwner>()

/**
 * The cached lifecycle owner of this controller
 */
val Controller.viewLifecycleOwner: LifecycleOwner
    get() {
        require(hasView) { "Cannot access viewLifecycleOwner while view == null" }
        return viewLifecycleOwnersByController.getOrPut(this) {
            ControllerViewLifecycleOwner(this)
                .also {
                    doOnPostUnbindView { viewLifecycleOwnersByController.remove(this) }
                }
        }
    }

/**
 * The view lifecycle of this controller
 */
val Controller.viewLifecycle: Lifecycle get() = viewLifecycleOwner.lifecycle