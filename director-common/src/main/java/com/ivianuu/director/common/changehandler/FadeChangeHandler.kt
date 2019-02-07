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
import com.ivianuu.director.handler

/**
 * A [AnimatorChangeHandler] that will cross fade two views
 */
open class FadeChangeHandler(
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
        val animator = AnimatorSet()

        if (to != null) {
            val start = (if (toAddedToContainer) 0f else to.alpha).toFloat()
            animator.play(ObjectAnimator.ofFloat(to, View.ALPHA, start, 1f))
        }

        if (from != null && (!isPush || removesFromViewOnPush)) {
            animator.play(ObjectAnimator.ofFloat(from, View.ALPHA, 0f))
        }

        return animator
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f
    }

    override fun copy() =
        FadeChangeHandler(
            duration,
            removesFromViewOnPush
        )
}

fun RouterTransactionBuilder.fade(
    duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    handler(FadeChangeHandler(duration, removesFromViewOnPush))
}

fun RouterTransactionBuilder.fadePush(
    duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    pushHandler(FadeChangeHandler(duration, removesFromViewOnPush))
}

fun RouterTransactionBuilder.fadePop(
    duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    popHandler(FadeChangeHandler(duration, removesFromViewOnPush))
}