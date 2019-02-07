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

package com.ivianuu.director.common.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.RouterTransactionBuilder

/**
 * A [AnimatorChangeHandler] that will slide the views left or right, depending on if it's a push or pop.
 */
open class HorizontalChangeHandler(
    duration: Long = DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
) : AnimatorChangeHandler(duration, removesFromViewOnPush) {

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator {
        val animatorSet = AnimatorSet()

        if (isPush) {
            if (from != null) {
                animatorSet.play(
                    ObjectAnimator.ofFloat(
                        from,
                        View.TRANSLATION_X,
                        -from.width.toFloat()
                    )
                )
            }
            if (to != null) {
                animatorSet.play(
                    ObjectAnimator.ofFloat(
                        to,
                        View.TRANSLATION_X,
                        to.width.toFloat(),
                        0f
                    )
                )
            }
        } else {
            if (from != null) {
                animatorSet.play(
                    ObjectAnimator.ofFloat(
                        from,
                        View.TRANSLATION_X,
                        from.width.toFloat()
                    )
                )
            }
            if (to != null) {
                // Allow this to have a nice transition when coming off an aborted push animation
                val fromLeft = from?.translationX ?: 0f
                animatorSet.play(
                    ObjectAnimator.ofFloat(
                        to,
                        View.TRANSLATION_X,
                        fromLeft - to.width.toFloat(),
                        0f
                    )
                )
            }
        }

        return animatorSet
    }

    override fun resetFromView(from: View) {
        from.translationX = 0f
    }

    override fun copy() =
        HorizontalChangeHandler(
            duration,
            removesFromViewOnPush
        )
}

fun RouterTransactionBuilder.horizontal(
    duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    handler(HorizontalChangeHandler(duration, removesFromViewOnPush))
}

fun RouterTransactionBuilder.horizontalPush(
    duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    pushHandler(HorizontalChangeHandler(duration, removesFromViewOnPush))
}

fun RouterTransactionBuilder.horizontalPop(
    duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    popHandler(HorizontalChangeHandler(duration, removesFromViewOnPush))
}