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
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.DirectorPlugins
import com.ivianuu.director.common.ChangeData
import com.ivianuu.director.common.OnReadyOrAbortedListener

/**
 * A base [ControllerChangeHandler] that uses [android.animation.Animator]s to replace Controller Views
 */
abstract class AnimatorChangeHandler(
    duration: Long = DirectorPlugins.defaultAnimationDuration,
    override var removesFromViewOnPush: Boolean = true
) : ControllerChangeHandler() {

    var duration = duration
        private set

    private var animator: Animator? = null
    private var onReadyOrAbortedListener: OnReadyOrAbortedListener? = null
    private var changeData: ChangeData? = null

    private var canceled = false
    private var needsImmediateCompletion = false
    private var completed = false

    init {
        this.duration = duration
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
        changeData = ChangeData(container, from, to, toIndex, isPush, onChangeComplete)

        var readyToAnimate = true
        val addingToView = to != null && to.parent == null

        if (addingToView) {
            container.addView(to, toIndex)
            if (!canceled && !completed && to!!.width <= 0 && to.height <= 0) {
                readyToAnimate = false
                onReadyOrAbortedListener = OnReadyOrAbortedListener(to) {
                    performAnimation(
                        container,
                        from,
                        to,
                        toIndex,
                        isPush,
                        true,
                        onChangeComplete
                    )
                }
            }
        }

        if (readyToAnimate) {
            performAnimation(container, from, to, toIndex, isPush, addingToView, onChangeComplete)
        }
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putLong(KEY_DURATION, duration)
        bundle.putBoolean(KEY_REMOVES_FROM_ON_PUSH, removesFromViewOnPush)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        duration = bundle.getLong(KEY_DURATION)
        removesFromViewOnPush = bundle.getBoolean(KEY_REMOVES_FROM_ON_PUSH)
    }

    override fun cancel(immediate: Boolean) {
        super.cancel(immediate)
        canceled = true
        needsImmediateCompletion = immediate

        when {
            animator != null -> {
                if (immediate) {
                    animator?.end()
                } else {
                    animator?.cancel()
                }
            }
            onReadyOrAbortedListener != null -> onReadyOrAbortedListener?.onReadyOrAborted()
            changeData != null -> complete(null)
        }
    }

    /**
     * Should be overridden to return the Animator to use while replacing Views.
     */
    protected abstract fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator

    /**
     * Will be called after the animation is complete to reset the View that was removed to its pre-animation state.
     */
    protected abstract fun resetFromView(from: View)

    private fun performAnimation(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        isPush: Boolean,
        toAddedToContainer: Boolean,
        onChangeComplete: () -> Unit
    ) {
        if (canceled) {
            complete(null)
            return
        }

        animator = getAnimator(container, from, to, toIndex, isPush, toAddedToContainer).apply {
            if (this@AnimatorChangeHandler.duration != NO_DURATION) {
                duration = this@AnimatorChangeHandler.duration
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    complete(this)
                }
            })
        }

        animator?.start()
    }

    private fun complete(animatorListener: Animator.AnimatorListener?) {
        if (completed) return
        completed = true

        val (container, from, to, _, isPush, onChangeComplete) = changeData!!

        if (canceled && !needsImmediateCompletion) {
            if (from != null) {
                resetFromView(from)
            }

            if (to != null && to.parent == container) {
                container.removeView(to)
            }
        } else {
            if (from != null && (!isPush || removesFromViewOnPush)) {
                container.removeView(from)
            }

            if (isPush && from != null) {
                resetFromView(from)
            }
        }

        onChangeComplete()

        animator?.let { animator ->
            animatorListener?.let { animator.removeListener(it) }
            animator.cancel()
        }

        animator = null
        onReadyOrAbortedListener = null
        changeData = null
    }

    companion object {
        private const val KEY_DURATION = "AnimatorChangeHandler.duration"
        private const val KEY_REMOVES_FROM_ON_PUSH = "AnimatorChangeHandler.removesFromViewOnPush"

        const val NO_DURATION = -1L
    }
}

private var _defaultAnimationDuration = AnimatorChangeHandler.NO_DURATION

/**
 * The default animation duration to use in all [AnimatorChangeHandler]s
 */
var DirectorPlugins.defaultAnimationDuration: Long
    get() = _defaultAnimationDuration
    set(value) {
        _defaultAnimationDuration = value
    }