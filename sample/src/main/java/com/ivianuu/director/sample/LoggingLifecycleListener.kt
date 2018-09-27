package com.ivianuu.director.sample

import android.content.Context
import android.os.Bundle
import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.ControllerLifecycleListener
import com.ivianuu.director.internal.d

/**
 * @author Manuel Wrage (IVIanuu)
 */
class LoggingLifecycleListener : ControllerLifecycleListener {

    override fun preCreate(controller: Controller) {
        super.preCreate(controller)
        controller.d { "Lifecycle: pre create" }
    }

    override fun postCreate(controller: Controller) {
        super.postCreate(controller)
        controller.d { "Lifecycle: post create" }
    }

    override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
        super.onRestoreInstanceState(controller, savedInstanceState)
        controller.d { "Lifecycle: on restore instance state" }
    }

    override fun preContextAvailable(controller: Controller) {
        super.preContextAvailable(controller)
        controller.d { "Lifecycle: pre context available" }
    }

    override fun postContextAvailable(controller: Controller, context: Context) {
        super.postContextAvailable(controller, context)
        controller.d { "Lifecycle: post context available" }
    }

    override fun preCreateView(controller: Controller) {
        super.preCreateView(controller)
        controller.d { "Lifecycle: pre create view" }
    }

    override fun postCreateView(controller: Controller, view: View) {
        super.postCreateView(controller, view)
        controller.d { "Lifecycle: post create view" }
    }

    override fun onRestoreViewState(controller: Controller, savedViewState: Bundle) {
        super.onRestoreViewState(controller, savedViewState)
        controller.d { "Lifecycle: on restore view state" }
    }

    override fun preAttach(controller: Controller, view: View) {
        super.preAttach(controller, view)
        controller.d { "Lifecycle: pre attach" }
    }

    override fun postAttach(controller: Controller, view: View) {
        super.postAttach(controller, view)
        controller.d { "Lifecycle: post attach" }
    }

    override fun preDetach(controller: Controller, view: View) {
        super.preDetach(controller, view)
        controller.d { "Lifecycle: pre detach" }
    }

    override fun postDetach(controller: Controller, view: View) {
        super.postDetach(controller, view)
        controller.d { "Lifecycle: post detach" }
    }

    override fun onSaveViewState(controller: Controller, outState: Bundle) {
        super.onSaveViewState(controller, outState)
        controller.d { "Lifecycle: on save view state" }
    }

    override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        super.onSaveInstanceState(controller, outState)
        controller.d { "Lifecycle: on save instance state" }
    }

    override fun preDestroyView(controller: Controller, view: View) {
        super.preDestroyView(controller, view)
        controller.d { "Lifecycle: pre destroy view" }
    }

    override fun postDestroyView(controller: Controller) {
        super.postDestroyView(controller)
        controller.d { "Lifecycle: post destroy view" }
    }

    override fun preContextUnavailable(controller: Controller, context: Context) {
        super.preContextUnavailable(controller, context)
        controller.d { "Lifecycle: pre context unavailable" }
    }

    override fun postContextUnavailable(controller: Controller) {
        super.postContextUnavailable(controller)
        controller.d { "Lifecycle: post context unavailable" }
    }

    override fun preDestroy(controller: Controller) {
        super.preDestroy(controller)
        controller.d { "Lifecycle: pre destroy" }
    }

    override fun postDestroy(controller: Controller) {
        super.postDestroy(controller)
        controller.d { "Lifecycle: post destroy" }
    }

    override fun onChangeStart(
        controller: Controller,
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeStart(controller, changeHandler, changeType)
        controller.d { "Lifecycle: on change start" }
    }

    override fun onChangeEnd(
        controller: Controller,
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnd(controller, changeHandler, changeType)
        controller.d { "Lifecycle: on change end" }
    }
}