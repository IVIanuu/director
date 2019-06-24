package com.ivianuu.director.sample.util

import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener

/**
 * @author Manuel Wrage (IVIanuu)
 */
class LoggingControllerLifecycleListener : ControllerLifecycleListener {

    private fun Controller.d(m: () -> String) {
        (this as Any).d { "Lifecycle: ${m.invoke()}" }
    }

    override fun preCreate(controller: Controller) {
        controller.d { "pre create" }
    }

    override fun postCreate(controller: Controller) {
        controller.d { "post create" }
    }

    override fun preCreateView(controller: Controller) {
        controller.d { "pre create view" }
    }

    override fun postCreateView(controller: Controller, view: View) {
        controller.d { "post create view" }
    }

    override fun preAttach(controller: Controller, view: View) {
        controller.d { "pre attach" }
    }

    override fun postAttach(controller: Controller, view: View) {
        controller.d { "post attach" }
    }

    override fun preDetach(controller: Controller, view: View) {
        controller.d { "pre detach" }
    }

    override fun postDetach(controller: Controller, view: View) {
        controller.d { "post detach" }
    }

    override fun preDestroyView(controller: Controller, view: View) {
        controller.d { "pre destroy view" }
    }

    override fun postDestroyView(controller: Controller) {
        controller.d { "post destroy view" }
    }

    override fun preDestroy(controller: Controller) {
        controller.d { "pre destroy" }
    }

    override fun postDestroy(controller: Controller) {
        controller.d { "post destroy" }
    }

}