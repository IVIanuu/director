package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.*

internal object ControllerChangeManager {

    private val handlers = mutableMapOf<String, ChangeHandler>()

    fun executeChange(
        router: Router,
        from: Controller?,
        to: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler?,
        forceRemoveFromViewOnPush: Boolean,
        listeners: List<RouterListener>
    ) {
        val handlerToUse = when {
            handler == null -> DefaultChangeHandler()
            handler.hasBeenUsed -> handler.copy()
            else -> handler
        }
        handlerToUse.hasBeenUsed = true

        if (from != null) {
            cancelChange(from.instanceId)
        }

        if (to != null) {
            handlers[to.instanceId] = handlerToUse
        }

        listeners.forEach { it.onChangeStarted(router, to, from, isPush, container, handlerToUse) }

        val toChangeType =
            if (isPush) ControllerChangeType.PUSH_ENTER else ControllerChangeType.POP_ENTER

        val fromChangeType =
            if (isPush) ControllerChangeType.PUSH_EXIT else ControllerChangeType.POP_EXIT

        val toView = to?.createView(container)
        to?.changeStarted(from, handlerToUse, toChangeType)

        val fromView = from?.view
        from?.changeStarted(to, handlerToUse, fromChangeType)

        val onChangeComplete: () -> Unit = {
            from?.changeEnded(to, handlerToUse, fromChangeType)

            if (to != null) {
                handlers.remove(to.instanceId)
                to.changeEnded(from, handlerToUse, toChangeType)
            }

            listeners.forEach {
                it.onChangeCompleted(
                    router,
                    to,
                    from,
                    isPush,
                    container,
                    handlerToUse
                )
            }
        }

        val changeData = ChangeData(
            container,
            fromView,
            toView,
            isPush,
            onChangeComplete,
            getToIndex(router, container, toView, fromView, isPush),
            forceRemoveFromViewOnPush
        )

        handlerToUse.performChange(changeData)
    }

    fun cancelChange(instanceId: String) {
        handlers.remove(instanceId)?.cancel()
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
                .map(container::getChildAt)
                .indexOfFirst { v ->
                    router.backstack.indexOfFirst {
                        it.controller.view == v
                    } > backstackIndex
                }
        } else {
            val currentToIndex = container.indexOfChild(to)
            val currentFromIndex = container.indexOfChild(from)

            if (currentToIndex == -1 || currentToIndex > currentFromIndex) {
                container.indexOfChild(from)
            } else {
                currentToIndex
            }
        }
    }

}