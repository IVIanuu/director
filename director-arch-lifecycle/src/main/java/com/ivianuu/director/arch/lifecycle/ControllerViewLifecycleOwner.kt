package com.ivianuu.director.arch.lifecycle

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener

/**
 * A [LifecycleOwner] for [Controller]s views
 */
class ControllerViewLifecycleOwner(controller: Controller) : LifecycleOwner {

    private var lifecycleRegistry: LifecycleRegistry? = null

    private val lifecycleListener = object : ControllerLifecycleListener {

        override fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
            super.preBindView(controller, view, savedViewState)
            lifecycleRegistry = LifecycleRegistry(this@ControllerViewLifecycleOwner)
        }

        override fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
            super.postBindView(controller, view, savedViewState)
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

        override fun preUnbindView(controller: Controller, view: View) {
            super.preUnbindView(controller, view)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        override fun postUnbindView(controller: Controller) {
            super.postUnbindView(controller)
            lifecycleRegistry = null
        }
    }

    init {
        controller.addLifecycleListener(lifecycleListener)
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
        ?: error("only accessible between onBindView and onUnbindView")
}

/**
 * Returns a new [ControllerViewLifecycleOwner] for [this]
 */
fun Controller.ControllerViewLifecycleOwner(): ControllerViewLifecycleOwner = ControllerViewLifecycleOwner(this)