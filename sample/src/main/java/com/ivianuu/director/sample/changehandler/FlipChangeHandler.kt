package com.ivianuu.director.sample.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Property
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator

import com.ivianuu.director.common.AnimatorChangeHandler

class FlipChangeHandler(
    private val flipDirection: FlipDirection = FlipDirection.RIGHT,
    private val animationDuration: Long = DEFAULT_ANIMATION_DURATION
) : AnimatorChangeHandler() {

    enum class FlipDirection(
        internal val inStartRotation: Float,
        internal val outEndRotation: Float,
        internal val property: Property<View, Float>
    ) {
        LEFT(-180f, 180f, View.ROTATION_Y),
        RIGHT(180f, -180f, View.ROTATION_Y),
        UP(-180f, 180f, View.ROTATION_X),
        DOWN(180f, -180f, View.ROTATION_X)
    }

    constructor(animationDuration: Long) : this(FlipDirection.RIGHT, animationDuration) {}

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator {
        val animatorSet = AnimatorSet()

        if (to != null) {
            to.alpha = 0f

            val rotation = ObjectAnimator.ofFloat<View>(
                to,
                flipDirection.property,
                flipDirection.inStartRotation,
                0f
            ).setDuration(animationDuration)
            rotation.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.play(rotation)

            val alpha =
                ObjectAnimator.ofFloat<View>(to, View.ALPHA, 1f).setDuration(animationDuration / 2)
            alpha.startDelay = animationDuration / 3
            animatorSet.play(alpha)
        }

        if (from != null) {
            val rotation = ObjectAnimator.ofFloat<View>(
                from,
                flipDirection.property,
                0f,
                flipDirection.outEndRotation
            ).setDuration(animationDuration)
            rotation.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.play(rotation)

            val alpha =
                ObjectAnimator.ofFloat<View>(from, View.ALPHA, 0f).setDuration(animationDuration / 2)
            alpha.startDelay = animationDuration / 3
            animatorSet.play(alpha)
        }

        return animatorSet
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f

        when (flipDirection) {
            FlipChangeHandler.FlipDirection.LEFT, FlipChangeHandler.FlipDirection.RIGHT -> from.rotationY =
                    0f
            FlipChangeHandler.FlipDirection.UP, FlipChangeHandler.FlipDirection.DOWN -> from.rotationX =
                    0f
        }
    }

    companion object {

        private val DEFAULT_ANIMATION_DURATION: Long = 300
    }
}
