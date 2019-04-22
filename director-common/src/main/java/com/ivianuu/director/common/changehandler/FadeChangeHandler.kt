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
import com.ivianuu.director.ChangeData
import com.ivianuu.director.DirectorPlugins
import com.ivianuu.director.defaultRemovesFromViewOnPush

/**
 * A [AnimatorChangeHandler] that will cross fade two views
 */
open class FadeChangeHandler(
    duration: Long = DirectorPlugins.defaultAnimationDuration,
    removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
) : AnimatorChangeHandler(duration, removesFromViewOnPush) {

    private var addedToView = false

    override fun performChange(changeData: ChangeData) {
        addedToView = changeData.to != null && changeData.to!!.parent == null
        super.performChange(changeData)
    }

    override fun getAnimator(changeData: ChangeData): Animator {
        val (_, from, to, isPush) = changeData

        val animator = AnimatorSet()

        if (to != null) {
            val start = if (addedToView) 0f else to.alpha
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

}