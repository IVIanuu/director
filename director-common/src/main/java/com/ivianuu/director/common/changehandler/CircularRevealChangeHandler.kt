package com.ivianuu.director.common.changehandler

import android.animation.Animator
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import com.ivianuu.director.DirectorPlugins

/**
 * A [AnimatorChangeHandler] that will perform a circular reveal
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
open class CircularRevealChangeHandler :
    AnimatorChangeHandler {

    private var cx = 0
    private var cy = 0

    constructor()

    constructor(
        fromView: View,
        containerView: View,
        duration: Long = DirectorPlugins.defaultAnimationDuration,
        removesFromViewOnPush: Boolean = true
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
        duration: Long = NO_DURATION,
        removesFromViewOnPush: Boolean = true
    ) : super(duration, removesFromViewOnPush) {
        this.cx = cx
        this.cy = cy
    }

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator {
        val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
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

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putInt(KEY_CX, cx)
        bundle.putInt(KEY_CY, cy)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        cx = bundle.getInt(KEY_CX)
        cy = bundle.getInt(KEY_CY)
    }

    companion object {
        private const val KEY_CX = "CircularRevealChangeHandler.cx"
        private const val KEY_CY = "CircularRevealChangeHandler.cy"
    }
}