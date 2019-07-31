/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director.sample.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Property
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.ivianuu.director.ChangeData
import com.ivianuu.director.common.changehandler.AnimatorChangeHandler

class FlipChangeHandler(
    private val flipDirection: FlipDirection = FlipDirection.RIGHT,
    private val animationDuration: Long = DEFAULT_ANIMATION_DURATION
) : AnimatorChangeHandler() {

    enum class FlipDirection(
        val inStartRotation: Float,
        val outEndRotation: Float,
        val property: Property<View, Float>
    ) {
        LEFT(-180f, 180f, View.ROTATION_Y),
        RIGHT(180f, -180f, View.ROTATION_Y),
        UP(-180f, 180f, View.ROTATION_X),
        DOWN(180f, -180f, View.ROTATION_X)
    }

    override fun getAnimator(changeData: ChangeData): Animator {
        val (_, from, to, isPush) = changeData

        val animatorSet = AnimatorSet()

        if (to != null) {
            to.alpha = 0f

            val rotation = ObjectAnimator.ofFloat(
                to,
                flipDirection.property,
                flipDirection.inStartRotation,
                0f
            ).setDuration(animationDuration)
            rotation.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.play(rotation)

            val alpha =
                ObjectAnimator.ofFloat(to, View.ALPHA, 1f).setDuration(animationDuration / 2)
            alpha.startDelay = animationDuration / 3
            animatorSet.play(alpha)
        }

        if (from != null) {
            val rotation = ObjectAnimator.ofFloat(
                from,
                flipDirection.property,
                0f,
                flipDirection.outEndRotation
            ).setDuration(animationDuration)
            rotation.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.play(rotation)

            val alpha =
                ObjectAnimator.ofFloat(from, View.ALPHA, 0f).setDuration(animationDuration / 2)
            alpha.startDelay = animationDuration / 3
            animatorSet.play(alpha)
        }

        return animatorSet
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f

        when (flipDirection) {
            FlipDirection.LEFT, FlipDirection.RIGHT -> from.rotationY =
                0f
            FlipDirection.UP, FlipDirection.DOWN -> from.rotationX =
                0f
        }
    }

    override fun copy() = FlipChangeHandler(flipDirection, animationDuration)

    companion object {
        private const val DEFAULT_ANIMATION_DURATION = 300L
    }
}
