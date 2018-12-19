package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.*

internal class ControllerChangeManager {

    private val inProgressChangeHandlers = mutableMapOf<String, ChangeHandlerData>()

    fun executeChange(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler?,
        listeners: List<ControllerChangeListener>
    ) {
        val handler = when {
            handler == null -> SimpleSwapChangeHandler()
            handler.hasBeenUsed -> handler.copy()
            else -> handler
        }
        handler.hasBeenUsed = true

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

            if (handler.forceRemoveViewOnPush && fromView != null) {
                val fromParent = fromView.parent
                if (fromParent != null && fromParent is ViewGroup) {
                    fromParent.removeView(fromView)
                }
            }
        }
    }

    fun completeChangeImmediately(controllerInstanceId: String) {
        inProgressChangeHandlers.remove(controllerInstanceId)
            ?.changeHandler?.completeImmediately()
    }

    private fun abortOrCompleteChange(
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

}

private data class ChangeHandlerData(
    val changeHandler: ControllerChangeHandler,
    val isPush: Boolean
)