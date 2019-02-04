package com.ivianuu.director.internal

import android.os.Bundle
import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.ControllerLifecycleListener
import com.ivianuu.director.RouterTransaction

fun <T : Any> newInstanceOrThrow(className: String): T = try {
    classForNameOrThrow<T>(className).newInstance() as T
} catch (e: Exception) {
    throw RuntimeException("could not instantiate $className, $e")
}

fun <T> classForNameOrThrow(className: String): Class<out T> = try {
    Class.forName(className) as Class<out T>
} catch (e: Exception) {
    throw RuntimeException("couldn't find class $className")
}

internal fun List<RouterTransaction>.filterVisible(): List<RouterTransaction> {
    val visible = mutableListOf<RouterTransaction>()

    for (transaction in reversed()) {
        visible.add(transaction)
        if (transaction.pushChangeHandler == null
            || transaction.pushChangeHandler!!.removesFromViewOnPush
        ) {
            break
        }
    }

    return visible.reversed()
}

internal fun backstacksAreEqual(
    lhs: List<RouterTransaction>,
    rhs: List<RouterTransaction>
): Boolean {
    if (lhs.size != rhs.size) return false

    lhs.forEachIndexed { i, transaction ->
        if (transaction != rhs[i]) return false
    }

    return true
}

internal fun LambdaLifecycleListener(
    preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    preInflateView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    postInflateView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    preBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    postBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    preUnbindView: ((controller: Controller, view: View) -> Unit)? = null,
    postUnbindView: ((controller: Controller) -> Unit)? = null,
    preDestroy: ((controller: Controller) -> Unit)? = null,
    postDestroy: ((controller: Controller) -> Unit)? = null,
    onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onSaveViewState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onChangeStart: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerLifecycleListener {
    return object : ControllerLifecycleListener {
        override fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
            preCreate?.invoke(controller, savedInstanceState)
        }

        override fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
            postCreate?.invoke(controller, savedInstanceState)
        }

        override fun preInflateView(controller: Controller, savedViewState: Bundle?) {
            preInflateView?.invoke(controller, savedViewState)
        }

        override fun postInflateView(controller: Controller, view: View, savedViewState: Bundle?) {
            postInflateView?.invoke(controller, view, savedViewState)
        }

        override fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
            preBindView?.invoke(controller, view, savedViewState)
        }

        override fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
            postBindView?.invoke(controller, view, savedViewState)
        }

        override fun preAttach(controller: Controller, view: View) {
            preAttach?.invoke(controller, view)
        }

        override fun postAttach(controller: Controller, view: View) {
            postAttach?.invoke(controller, view)
        }

        override fun preDetach(controller: Controller, view: View) {
            preDetach?.invoke(controller, view)
        }

        override fun postDetach(controller: Controller, view: View) {
            postDetach?.invoke(controller, view)
        }

        override fun preUnbindView(controller: Controller, view: View) {
            preUnbindView?.invoke(controller, view)
        }

        override fun postUnbindView(controller: Controller) {
            postUnbindView?.invoke(controller)
        }

        override fun preDestroy(controller: Controller) {
            preDestroy?.invoke(controller)
        }

        override fun postDestroy(controller: Controller) {
            postDestroy?.invoke(controller)
        }

        override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
            onSaveInstanceState?.invoke(controller, outState)
        }

        override fun onSaveViewState(controller: Controller, outState: Bundle) {
            onSaveViewState?.invoke(controller, outState)
        }

        override fun onChangeStart(
            controller: Controller,
            changeHandler: ControllerChangeHandler,
            changeType: ControllerChangeType
        ) {
            onChangeStart?.invoke(controller, changeHandler, changeType)
        }

        override fun onChangeEnd(
            controller: Controller,
            changeHandler: ControllerChangeHandler,
            changeType: ControllerChangeType
        ) {
            onChangeEnd?.invoke(controller, changeHandler, changeType)
        }
    }
}