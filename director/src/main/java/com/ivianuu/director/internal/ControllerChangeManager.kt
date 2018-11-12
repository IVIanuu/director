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

    fun completeChangeImmediately(controllerInstanceId: String): Boolean {
        val changeHandlerData = inProgressChangeHandlers[controllerInstanceId]
        if (changeHandlerData != null) {
            changeHandlerData.changeHandler.completeImmediately()
            inProgressChangeHandlers.remove(controllerInstanceId)
            return true
        }
        return false
    }

    fun abortOrCompleteChange(
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

    fun executeChange(transaction: ChangeTransaction) {
        val (to, from, isPush, container, inHandler,
                listeners) = transaction

        if (container == null) return

        val handler = inHandler?.copy() ?: SimpleSwapChangeHandler()

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
            from?.changeEnded(handler, fromChangeType)

            if (to != null) {
                inProgressChangeHandlers.remove(to.instanceId)
                to.changeEnded(handler, toChangeType)
            }

            listeners.forEach { it.onChangeCompleted(to, from, isPush, container, handler) }

            if (handler.removesFromViewOnPush && fromView != null) {
                (fromView.parent as? ViewGroup)?.removeView(fromView)
                from!!.needsAttach = false
            }
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