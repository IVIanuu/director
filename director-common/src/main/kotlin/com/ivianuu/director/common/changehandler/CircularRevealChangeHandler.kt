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