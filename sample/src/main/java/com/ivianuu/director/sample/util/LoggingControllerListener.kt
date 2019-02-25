package com.ivianuu.director.sample.util

import android.os.Bundle
import android.view.View
import com.ivianuu.director.ChangeHandler
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.ControllerListener

/**
 * @author Manuel Wrage (IVIanuu)
 */
class LoggingControllerListener : ControllerListener {

    private fun Controller.d(m: () -> String) {
        (this as Any).d { "Lifecycle: ${m.invoke()}" }
    }

    override fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
        controller.d { "pre create" }
    }

    override fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
        controller.d { "post create" }
    }

    override fun preBuildView(controller: Controller, savedViewState: Bundle?) {
        controller.d { "pre build view" }
    }

    override fun postBuildView(controller: Controller, view: View, savedViewState: Bundle?) {
        controller.d { "post build view" }
    }

    override fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
        controller.d { "pre bind view" }
    }

    override fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
        controller.d { "post bind view" }
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

    override fun preUnbindView(controller: Controller, view: View) {
        controller.d { "pre unbind view" }
    }

    override fun postUnbindView(controller: Controller) {
        controller.d { "post unbind view" }
    }

    override fun preDestroy(controller: Controller) {
        controller.d { "pre destroy" }
    }

    override fun postDestroy(controller: Controller) {
        controller.d { "post destroy" }
    }

    override fun onChangeStart(
        controller: Controller,
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        controller.d { "on change start -> $changeType, $changeHandler" }
    }

    override fun onChangeEnd(
        controller: Controller,
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        controller.d { "on change end -> $changeType, $changeHandler" }
    }

    override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
        controller.d { "on restore instance state" }
    }

    override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        controller.d { "on save instance state" }
    }

    override fun onRestoreViewState(controller: Controller, view: View, savedViewState: Bundle) {
        controller.d { "on restore view state" }
    }

    override fun onSaveViewState(controller: Controller, view: View, outState: Bundle) {
        controller.d { "on save view state" }
    }
}