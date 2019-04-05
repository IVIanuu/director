package com.ivianuu.director.internal

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
        toIndex: Int,
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

        val toView = to?.view ?: to?.createView(container)
        val fromView = from?.view

        val callback = object : ChangeHandler.Callback {
            override fun addToView() {
                val addingToView = toView != null && toView.parent == null
                val movingToView = toView != null && container.indexOfChild(toView) != toIndex
                if (addingToView) {
                    container.addView(toView, toIndex)
                } else if (movingToView) {
                    container.moveView(toView!!, toIndex)
                }
            }

            override fun removeFromView() {
                if (fromView != null && (!isPush || handlerToUse.removesFromViewOnPush
                            || forceRemoveFromViewOnPush)
                ) {
                    container.removeView(fromView)
                }
            }

            override fun onChangeCompleted() {
                if (to != null) {
                    handlers.remove(to.instanceId)
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
        }

        val changeData = ChangeData(
            container,
            fromView,
            toView,
            isPush,
            callback,
            toIndex,
            forceRemoveFromViewOnPush
        )

        handlerToUse.performChange(changeData)
    }

    fun cancelChange(instanceId: String) {
        handlers.remove(instanceId)?.cancel()
    }

}