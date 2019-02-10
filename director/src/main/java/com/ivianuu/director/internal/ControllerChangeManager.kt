package com.ivianuu.director.internal

import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeListener
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.SimpleSwapChangeHandler

internal object ControllerChangeManager {

    private val inProgressChangeHandlers = mutableMapOf<String, ChangeHandlerData>()

    fun executeChange(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler?,
        listeners: List<ControllerChangeListener>
    ) {
        val handlerToUse = when {
            handler == null -> SimpleSwapChangeHandler()
            handler.hasBeenUsed -> handler.copy()
            else -> handler
        }
        handlerToUse.hasBeenUsed = true

        if (from != null) {
            cancelChange(from.instanceId, isPush)
        }

        if (to != null) {
            inProgressChangeHandlers[to.instanceId] = ChangeHandlerData(handlerToUse, isPush)
        }

        listeners.forEach { it.onChangeStarted(to, from, isPush, container, handlerToUse) }

        val toChangeType =
            if (isPush) ControllerChangeType.PUSH_ENTER else ControllerChangeType.POP_ENTER

        val fromChangeType =
            if (isPush) ControllerChangeType.PUSH_EXIT else ControllerChangeType.POP_EXIT

        val toView = to?.inflate(container)
        to?.changeStarted(handlerToUse, toChangeType)

        val fromView = from?.view
        from?.changeStarted(handlerToUse, fromChangeType)

        handlerToUse.performChange(
            container,
            fromView,
            toView,
            isPush
        ) {
            from?.changeEnded(handlerToUse, fromChangeType)

            if (to != null) {
                inProgressChangeHandlers.remove(to.instanceId)
                to.changeEnded(handlerToUse, toChangeType)
            }

            listeners.forEach { it.onChangeCompleted(to, from, isPush, container, handlerToUse) }

            if (handlerToUse.forceRemoveViewOnPush && fromView != null) {
                val fromParent = fromView.parent
                if (fromParent != null && fromParent is ViewGroup) {
                    fromParent.removeView(fromView)
                }
            }
        }
    }

    fun cancelChange(instanceId: String, immediate: Boolean) {
        val changeHandlerData = inProgressChangeHandlers[instanceId]

        if (changeHandlerData != null) {
            if (immediate) {
                changeHandlerData.changeHandler.cancel(true)
            } else {
                if (changeHandlerData.isPush) {
                    changeHandlerData.changeHandler.cancel(false)
                } else {
                    changeHandlerData.changeHandler.cancel(true)
                }
            }

            inProgressChangeHandlers.remove(instanceId)
        }
    }

}

private data class ChangeHandlerData(
    val changeHandler: ControllerChangeHandler,
    val isPush: Boolean
)