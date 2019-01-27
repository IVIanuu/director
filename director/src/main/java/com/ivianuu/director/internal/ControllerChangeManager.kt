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
            completeChangeImmediately(from.instanceId)
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

    fun completeChangeImmediately(instanceId: String) {
        inProgressChangeHandlers.remove(instanceId)
            ?.changeHandler?.completeImmediately()
    }

}

private data class ChangeHandlerData(
    val changeHandler: ControllerChangeHandler,
    val isPush: Boolean
)