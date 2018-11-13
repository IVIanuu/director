package com.ivianuu.director.internal

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.ControllerLifecycleListener

fun <T : Any> newInstanceOrThrow(className: String) = try {
    classForNameOrThrow<T>(className).newInstance() as T
} catch (e: Exception) {
    throw RuntimeException("could not instantiate $className, $e")
}

fun <T> classForNameOrThrow(className: String) = try {
    Class.forName(className) as Class<out T>
} catch (e: Exception) {
    throw RuntimeException("couldn't find class $className")
}

internal fun requireMainThread() {
    if (Looper.getMainLooper() != Looper.myLooper()) {
        throw IllegalStateException("must be called from the main thread")
    }
}

@PublishedApi
internal val DEBUG = true

inline fun Any.d(m: () -> String) {
    if (DEBUG) {
        Log.d(javaClass.simpleName, m())
    }
}

internal class LoggingLifecycleListener : ControllerLifecycleListener {

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

    override fun preBindView(controller: Controller, view: View) {
        super.preBindView(controller, view)
        controller.d { "Lifecycle: pre bind view" }
    }

    override fun postBindView(controller: Controller, view: View) {
        super.postBindView(controller, view)
        controller.d { "Lifecycle: post bind view" }
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

    override fun preUnbindView(controller: Controller, view: View) {
        super.preUnbindView(controller, view)
        controller.d { "Lifecycle: pre unbind view" }
    }

    override fun postUnbindView(controller: Controller) {
        super.postUnbindView(controller)
        controller.d { "Lifecycle: post unbind view" }
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