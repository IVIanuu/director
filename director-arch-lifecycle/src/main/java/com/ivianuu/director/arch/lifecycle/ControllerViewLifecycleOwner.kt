package com.ivianuu.director.arch.lifecycle

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener

/**
 * A [LifecycleOwner] for [Controller]'s views
 */
class ControllerViewLifecycleOwner(controller: Controller) : LifecycleOwner {

    private var lifecycleRegistry: LifecycleRegistry? = null

    private val lifecycleListener = object : ControllerLifecycleListener {

        override fun preCreateView(controller: Controller) {
            super.preCreateView(controller)
            lifecycleRegistry = LifecycleRegistry(this@ControllerViewLifecycleOwner)
        }

        override fun postCreateView(controller: Controller, view: View) {
            super.postCreateView(controller, view)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        override fun postAttach(controller: Controller, view: View) {
            super.postAttach(controller, view)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun preDetach(controller: Controller, view: View) {
            super.preDetach(controller, view)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        override fun preDestroyView(controller: Controller, view: View) {
            super.preDestroyView(controller, view)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        override fun postDestroyView(controller: Controller) {
            super.postDestroyView(controller)
            lifecycleRegistry = null
        }
    }

    init {
        controller.addLifecycleListener(lifecycleListener)
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
        ?: throw IllegalStateException("only accessible between onCreateView and onDestroyView")
}

/**
 * Returns a new [ControllerViewLifecycleOwner] for [this]
 */
fun Controller.ControllerViewLifecycleOwner() = ControllerViewLifecycleOwner(this)