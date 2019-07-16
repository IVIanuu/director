package com.ivianuu.director.common.changehandler

import android.animation.Animator
import android.annotation.TargetApi
import android.view.View
import android.view.ViewAnimationUtils
import com.ivianuu.director.ChangeData
import com.ivianuu.director.DirectorPlugins
import com.ivianuu.director.defaultRemovesFromViewOnPush
import kotlin.math.hypot

/**
 * A [AnimatorChangeHandler] that will perform a circular reveal
 */
@TargetApi(21)
open class CircularRevealChangeHandler : AnimatorChangeHandler {

    private var cx = 0
    private var cy = 0

    constructor(
        fromView: View,
        containerView: View,
        duration: Long = DirectorPlugins.defaultAnimationDuration,
        removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
    ) : super(duration, removesFromViewOnPush) {
        val fromLocation = IntArray(2)
        fromView.getLocationInWindow(fromLocation)

        val containerLocation = IntArray(2)
        containerView.getLocationInWindow(containerLocation)

        val relativeLeft = fromLocation[0] - containerLocation[0]
        val relativeTop = fromLocation[1] - containerLocation[1]

        this.cx = fromView.width / 2 + relativeLeft
        this.cy = fromView.height / 2 + relativeTop
    }

    constructor(
        cx: Int,
        cy: Int,
        duration: Long = DirectorPlugins.defaultAnimationDuration,
        removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
    ) : super(duration, removesFromViewOnPush) {
        this.cx = cx
        this.cy = cy
    }

    override fun getAnimator(changeData: ChangeData): Animator {
        val (_, from, to, isPush) = changeData
        val radius = hypot(cx.toFloat(), cy.toFloat())
        var animator: Animator? = null
        if (isPush && to != null) {
            animator = ViewAnimationUtils.createCircularReveal(to, cx, cy, 0f, radius)
        } else if (!isPush && from != null) {
            animator = ViewAnimationUtils.createCircularReveal(from, cx, cy, radius, 0f)
        }
        return animator!!
    }

    override fun resetFromView(from: View) {
    }

    override fun copy() = CircularRevealChangeHandler(cx, cy, duration, removesFromViewOnPush)
}