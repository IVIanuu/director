package com.ivianuu.director.androidx.lifecycle

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener
import com.ivianuu.director.ControllerState
import com.ivianuu.director.doOnPostUnbindView
import com.ivianuu.director.isAtLeast

/**
 * A [LifecycleOwner] for [Controller]s views
 */
class ControllerViewLifecycleOwner(controller: Controller) : LifecycleOwner {

    private var lifecycleRegistry: LifecycleRegistry? = null

    private val lifecycleListener = object : ControllerLifecycleListener {

        override fun postInflateView(controller: Controller, view: View, savedViewState: Bundle?) {
            super.postInflateView(controller, view, savedViewState)
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

private val viewLifecycleOwnersByController = mutableMapOf<Controller, LifecycleOwner>()

/**
 * The cached lifecycle owner of this controller
 */
val Controller.viewLifecycleOwner: LifecycleOwner
    get() {
        require(state.isAtLeast(ControllerState.VIEW_BOUND)) {
            "Cannot access viewLifecycleOwner while view == null"
        }

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
val Controller.viewLifecycle: Lifecycle get() = lifecycleOwner.lifecycle