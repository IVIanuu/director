package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeListener
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.Router
import com.ivianuu.director.SimpleSwapChangeHandler

internal object ControllerChangeManager {

    private val handlers = mutableMapOf<String, ChangeHandlerData>()

    fun executeChange(
        router: Router,
        from: Controller?,
        to: Controller?,
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
            handlers[to.instanceId] = ChangeHandlerData(handlerToUse, isPush)
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
            getToIndex(router, container, toView, fromView, isPush),
            isPush
        ) {
            if (from != null) {
                handlers.remove(from.instanceId)
                from.changeEnded(handlerToUse, fromChangeType)
            }

            if (to != null) {
                handlers.remove(to.instanceId)
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
        val changeHandlerData = handlers[instanceId]

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

            handlers.remove(instanceId)
        }
    }

    private fun getToIndex(
        router: Router,
        container: ViewGroup,
        to: View?,
        from: View?,
        isPush: Boolean
    ): Int {
        if (to == null) return -1
        return if (isPush || from == null) {
            if (container.childCount == 0) return -1
            val backstackIndex = router.backstack.indexOfFirst { it.controller.view == to }
            (0 until container.childCount)
                .map { container.getChildAt(it) }
                .indexOfFirst { v ->
                    router.backstack.indexOfFirst {
                        it.controller.view == v
                    } > backstackIndex
                }
        } else {
            val currentToIndex = container.indexOfChild(to)
            val currentFromIndex = container.indexOfChild(from)

            if (currentToIndex > currentFromIndex) {
                container.indexOfChild(from)
            } else {
                currentToIndex
            }
        }
    }

}

private data class ChangeHandlerData(
    val changeHandler: ControllerChangeHandler,
    val isPush: Boolean
)