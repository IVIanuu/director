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
import com.ivianuu.director.*

/**
 * A [LifecycleOwner] for [Controller]s views
 */
class ControllerViewLifecycleOwner(controller: Controller) : LifecycleOwner {

    private var lifecycleRegistry: LifecycleRegistry? = null

    private val listener = ControllerListener(
        postCreateView = { _, _, _ ->
            lifecycleRegistry = LifecycleRegistry(this@ControllerViewLifecycleOwner)
            lifecycleRegistry?.handleLifecycleEvent(ON_CREATE)
        },
        postAttach = { _, _ ->
            lifecycleRegistry?.handleLifecycleEvent(ON_START)
            lifecycleRegistry?.handleLifecycleEvent(ON_RESUME)
        },
        preDetach = { _, _ ->
            lifecycleRegistry?.handleLifecycleEvent(ON_PAUSE)
            lifecycleRegistry?.handleLifecycleEvent(ON_STOP)
        },
        preDestroyView = { _, _ -> lifecycleRegistry?.handleLifecycleEvent(ON_DESTROY) },
        postDestroyView = { lifecycleRegistry = null }
    )

    init {
        controller.addListener(listener)

        if (controller.isViewCreated) {
            listener.postCreateView(controller, controller.view!!, null)
        }

        if (controller.isAttached) {
            listener.postAttach(controller, controller.view!!)
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
        ?: error("only accessible between onCreateView and onDestroyView")
}

private val viewLifecycleOwnersByController = mutableMapOf<Controller, LifecycleOwner>()

/**
 * The cached lifecycle owner of this controller
 */
val Controller.viewLifecycleOwner: LifecycleOwner
    get() {
        require(isViewCreated) { "Cannot access viewLifecycleOwner while view is not created" }
        return viewLifecycleOwnersByController.getOrPut(this) {
            ControllerViewLifecycleOwner(this)
                .also {
                    doOnpostDestroyView { viewLifecycleOwnersByController.remove(this) }
                }
        }
    }

/**
 * The view lifecycle of this controller
 */
val Controller.viewLifecycle: Lifecycle get() = viewLifecycleOwner.lifecycle