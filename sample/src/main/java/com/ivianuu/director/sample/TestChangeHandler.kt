package com.ivianuu.director.sample

import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import com.ivianuu.director.common.ViewPropertyChangeHandler

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TestChangeHandler(removesFromViewOnPush: Boolean = true) :
    ViewPropertyChangeHandler(removesFromViewOnPush = removesFromViewOnPush) {

    override fun getFromAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): ViewPropertyAnimator? {
        return if (from != null) {
            if (isPush) {
                from.animate().translationX(-from.width.toFloat())
            } else {
                from.animate().translationX(from.width.toFloat())
            }
        } else {
            null
        }
    }

    override fun getToAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): ViewPropertyAnimator? {
        return if (to != null) {
            if (isPush) {
                to.translationX = to.width.toFloat()
                to.animate().translationX(0f)
            } else {
                to.translationX = -to.width.toFloat()
                to.animate().translationX(0f)
            }
        } else {
            null
        }
    }

    override fun resetFromView(from: View) {
        from.translationX = 0f
    }
}