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

/**
 * An [AnimatorChangeHandler] that will slide either slide a new View up or slide an old View down,
 * depending on whether a push or pop change is happening.
 */
open class VerticalChangeHandler(
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
        val viewAnimators = mutableListOf<Animator>()

        if (isPush && to != null) {
            viewAnimators.add(
                ObjectAnimator.ofFloat<View>(
                    to,
                    View.TRANSLATION_Y,
                    to.height.toFloat(),
                    0f
                )
            )
        } else if (!isPush && from != null) {
            viewAnimators.add(
                ObjectAnimator.ofFloat<View>(
                    from,
                    View.TRANSLATION_Y,
                    from.height.toFloat()
                )
            )
        }

        animator.playTogether(viewAnimators)
        return animator
    }

    override fun resetFromView(from: View) {
    }

    override fun copy() =
        VerticalChangeHandler(
            duration,
            removesFromViewOnPush
        )
}