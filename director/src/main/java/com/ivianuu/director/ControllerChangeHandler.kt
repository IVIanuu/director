package com.ivianuu.director

import android.view.View
import android.view.ViewGroup
import java.util.*

/**
 * Swaps views on controller changes
 */
abstract class ControllerChangeHandler {

    /**
     * Whether or not this changeHandler removes the from view on push
     */
    open val removesFromViewOnPush: Boolean get() = true

    internal var hasBeenUsed = false

    /**
     * Responsible for swapping Views from one Controller to another.
     */
    abstract fun performChange(changeData: ChangeData)

    /**
     * Will be called when the change must be canceled
     */
    open fun cancel() {
    }

    internal open fun copy(): ControllerChangeHandler = this // todo

    interface Callback {
        fun addToView()
        fun attachToController()
        fun removeFromView()
        fun detachFromController()
        fun changeCompleted()
    }

}

fun ViewGroup.moveView(view: View, to: Int) {
    if (to == -1) {
        view.bringToFront()
        return
    }
    val index = indexOfChild(view)
    if (index == -1 || index == to) return
    val allViews = (0 until childCount).map { getChildAt(it) }.toMutableList()
    Collections.swap(allViews, index, to)
    allViews.forEach { it.bringToFront() }
    requestLayout()
    invalidate()
}