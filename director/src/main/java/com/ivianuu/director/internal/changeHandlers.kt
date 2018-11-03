package com.ivianuu.director.internal

import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeListener
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.SimpleSwapChangeHandler

internal data class ChangeTransaction(
    val to: Controller?,
    val from: Controller?,
    val isPush: Boolean,
    val container: ViewGroup?,
    val changeHandler: ControllerChangeHandler?,
    val listeners: List<ControllerChangeListener>
)

private data class ChangeHandlerData(
    val changeHandler: ControllerChangeHandler,
    val isPush: Boolean
)

private val inProgressChangeHandlers = mutableMapOf<String, ChangeHandlerData>()

internal fun completeChangeImmediately(controllerInstanceId: String): Boolean {
    val changeHandlerData = inProgressChangeHandlers[controllerInstanceId]
    if (changeHandlerData != null) {
        changeHandlerData.changeHandler.completeImmediately()
        inProgressChangeHandlers.remove(controllerInstanceId)
        return true
    }
    return false
}

internal fun abortOrCompleteChange(
    toAbort: Controller,
    newController: Controller?,
    newChangeHandler: ControllerChangeHandler
) {
    val changeHandlerData = inProgressChangeHandlers[toAbort.instanceId]
    if (changeHandlerData != null) {
        if (changeHandlerData.isPush) {
            changeHandlerData.changeHandler.onAbortPush(newChangeHandler, newController)
        } else {
            changeHandlerData.changeHandler.completeImmediately()
        }

        inProgressChangeHandlers.remove(toAbort.instanceId)
    }
}

internal fun executeChange(transaction: ChangeTransaction) {
    val (to, from, isPush, container, inHandler,
            listeners) = transaction

    d { "execute change $transaction" }

    if (container == null) return

    val handler = if (inHandler == null) {
        SimpleSwapChangeHandler()
    } else {
        inHandler.copy()
    }

    d { "handler is $handler" }

    if (from != null) {
        d { "from is not null" }
        if (isPush) {
            d { "complete change immediately" }
            completeChangeImmediately(from.instanceId)
        } else {
            d { "abort or complete" }
            abortOrCompleteChange(from, to, handler)
        }
    }

    if (to != null) {
        inProgressChangeHandlers[to.instanceId] = ChangeHandlerData(handler, isPush)
    }

    listeners.forEach { it.onChangeStarted(to, from, isPush, container, handler) }

    val toChangeType =
        if (isPush) ControllerChangeType.PUSH_ENTER else ControllerChangeType.POP_ENTER

    val fromChangeType =
        if (isPush) ControllerChangeType.PUSH_EXIT else ControllerChangeType.POP_EXIT

    d { "to change type $toChangeType, from change type $fromChangeType" }

    val toView: View?
    if (to != null) {
        d { "inflate to view" }
        toView = to.inflate(container)
        to.changeStarted(handler, toChangeType)
    } else {
        d { "to is null" }
        toView = null
    }

    val fromView: View?
    if (from != null) {
        d { "get from view" }
        fromView = from.view
        from.changeStarted(handler, fromChangeType)
    } else {
        d { "from is null" }
        fromView = null
    }

    handler.performChange(
        container,
        fromView,
        toView,
        isPush
    ) {
        from?.changeEnded(handler, fromChangeType)

        if (to != null) {
            inProgressChangeHandlers.remove(to.instanceId)
            to.changeEnded(handler, toChangeType)
        }

        listeners.forEach { it.onChangeCompleted(to, from, isPush, container, handler) }

        if (handler.removesFromViewOnPush && fromView != null) {
            d { "try to remove from view" }
            val fromParent = fromView.parent as? ViewGroup
            if (fromParent != null) {
                d { "remove from view" }
                fromParent.removeView(fromView)
            }
        }

        if (handler.removesFromViewOnPush && from != null) {
            d { "from needs attach" }
            from.needsAttach = false
        }
    }
}

private fun d(m: () -> String) { Log.d("ChangeHandlers", m()) }