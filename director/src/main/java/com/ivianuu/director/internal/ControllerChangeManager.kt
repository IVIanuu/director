package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeListener
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.SimpleSwapChangeHandler

internal class ControllerChangeManager {

    private val inProgressChangeHandlers = mutableMapOf<String, ChangeHandlerData>()

    fun executeChange(transaction: ChangeTransaction) {
        val (to, from, isPush, container, inHandler,
                listeners) = transaction

        d { "execute change $transaction" }

        if (container == null) return

        val handler = when {
            inHandler == null -> SimpleSwapChangeHandler()
            inHandler.hasBeenUsed -> inHandler.copy()
            else -> inHandler
        }
        handler.hasBeenUsed = true

        d { "handler $handler" }

        if (from != null) {
            if (isPush) {
                completeChangeImmediately(from.instanceId)
            } else {
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

        val toView: View?
        if (to != null) {
            toView = to.inflate(container)
            to.changeStarted(handler, toChangeType)
        } else {
            toView = null
        }

        val fromView: View?
        if (from != null) {
            fromView = from.view
            from.changeStarted(handler, fromChangeType)
        } else {
            fromView = null
        }

        handler.performChange(
            container,
            fromView,
            toView,
            isPush
        ) {
            d { "change ended $transaction" }
            from?.changeEnded(handler, fromChangeType)

            if (to != null) {
                inProgressChangeHandlers.remove(to.instanceId)
                to.changeEnded(handler, toChangeType)
            }

            listeners.forEach { it.onChangeCompleted(to, from, isPush, container, handler) }

            if (handler.forceRemoveViewOnPush && fromView != null) {
                val fromParent = fromView.parent
                if (fromParent != null && fromParent is ViewGroup) {
                    d { "force remove from view" }
                    fromParent.removeView(fromView)
                }
            }

            if (handler.removesFromViewOnPush && from != null) {
                d { "set from needs attach to false" }
                from.needsAttach = false
            }
        }
    }

    fun completeChangeImmediately(controllerInstanceId: String): Boolean {
        val changeHandlerData = inProgressChangeHandlers[controllerInstanceId]
        if (changeHandlerData != null) {
            d { "complete change immediately $controllerInstanceId" }
            changeHandlerData.changeHandler.completeImmediately()
            inProgressChangeHandlers.remove(controllerInstanceId)
            return true
        }

        d { "couldnt complete change immediately for $controllerInstanceId" }

        return false
    }

    private fun abortOrCompleteChange(
        toAbort: Controller,
        newController: Controller?,
        newChangeHandler: ControllerChangeHandler
    ) {
        d { "abort or complete change to abort $toAbort, new controller $newController, new change handler $newChangeHandler" }
        val changeHandlerData = inProgressChangeHandlers[toAbort.instanceId]

        if (changeHandlerData != null) {
            if (changeHandlerData.isPush) {
                d { "on abort push" }
                changeHandlerData.changeHandler.onAbortPush(newChangeHandler, newController)
            } else {
                d { "complete immediately" }
                changeHandlerData.changeHandler.completeImmediately()
            }

            inProgressChangeHandlers.remove(toAbort.instanceId)
        } else {
            d { "no change handler data found" }
        }
    }

}

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