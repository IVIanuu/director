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

package com.ivianuu.director.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler

/**
 * A base [ControllerChangeHandler] that facilitates using [android.animation.Animator]s to replace Controller Views
 */
abstract class AnimatorChangeHandler(
    duration: Long = DEFAULT_ANIMATION_DURATION,
    override var removesFromViewOnPush: Boolean = true
) : ControllerChangeHandler() {

    var duration =
        DEFAULT_ANIMATION_DURATION
        private set

    private var animator: Animator? = null
    private var onAnimationReadyOrAbortedListener: OnAnimationReadyOrAbortedListener? = null

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
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
        var readyToAnimate = true
        val addingToView = to != null && to.parent == null

        if (addingToView) {
            if (isPush || from == null) {
                container.addView(to)
            } else if (to?.parent == null) {
                container.addView(to, container.indexOfChild(from))
            }

            if (to!!.width <= 0 && to.height <= 0) {
                readyToAnimate = false
                onAnimationReadyOrAbortedListener = OnAnimationReadyOrAbortedListener(
                    container,
                    from,
                    to,
                    isPush,
                    true,
                    onChangeComplete
                )

                to.viewTreeObserver.addOnPreDrawListener(onAnimationReadyOrAbortedListener)
            }
        }

        if (readyToAnimate) {
            performAnimation(container, from, to, isPush, addingToView, onChangeComplete)
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

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)
        canceled = true

        if (animator != null) {
            animator?.cancel()
        } else if (onAnimationReadyOrAbortedListener != null) {
            onAnimationReadyOrAbortedListener?.onReadyOrAborted()
        }
    }

    override fun completeImmediately() {
        super.completeImmediately()
        needsImmediateCompletion = true
        if (animator != null) {
            animator?.end()
        } else if (onAnimationReadyOrAbortedListener != null) {
            onAnimationReadyOrAbortedListener?.onReadyOrAborted()
        }
    }

    /**
     * Should be overridden to return the Animator to use while replacing Views.
     */
    protected abstract fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
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
        isPush: Boolean,
        toAddedToContainer: Boolean,
        onChangeComplete: () -> Unit
    ) {
        if (canceled) {
            complete(onChangeComplete, null)
            return
        }

        if (needsImmediateCompletion) {
            if (from != null && (!isPush || removesFromViewOnPush)) {
                container.removeView(from)
            }
            complete(onChangeComplete, null)
            if (isPush && from != null) {
                resetFromView(from)
            }
            return
        }

        animator = getAnimator(container, from, to, isPush, toAddedToContainer).apply {
            if (this@AnimatorChangeHandler.duration > 0) {
                duration = this@AnimatorChangeHandler.duration
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    if (from != null) {
                        resetFromView(from)
                    }

                    if (to != null && to.parent == container) {
                        container.removeView(to)
                    }

                    complete(onChangeComplete, this)
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!canceled && animator != null) {
                        if (from != null && (!isPush || removesFromViewOnPush)) {
                            container.removeView(from)
                        }

                        complete(onChangeComplete, this)

                        if (isPush && from != null) {
                            resetFromView(from)
                        }
                    }
                }
            })

            start()
        }
    }

    private fun complete(
        onChangeComplete: () -> Unit,
        animatorListener: Animator.AnimatorListener?
    ) {
        if (!completed) {
            completed = true
            onChangeComplete()
        }

        animator?.let { animator ->
            animatorListener?.let { animator.removeListener(it) }
            animator.cancel()
        }

        animator = null
        onAnimationReadyOrAbortedListener = null
    }

    private inner class OnAnimationReadyOrAbortedListener(
        val container: ViewGroup,
        val from: View?,
        val to: View?,
        val isPush: Boolean,
        val addingToView: Boolean,
        val onChangeComplete: () -> Unit
    ) : ViewTreeObserver.OnPreDrawListener {

        private var hasRun = false

        override fun onPreDraw(): Boolean {
            onReadyOrAborted()
            return true
        }

        fun onReadyOrAborted() {
            if (hasRun) return
            hasRun = true

            if (to != null) {
                val observer = to.viewTreeObserver
                if (observer.isAlive) {
                    observer.removeOnPreDrawListener(this)
                }
            }

            performAnimation(container, from, to, isPush, addingToView, onChangeComplete)
        }
    }

    companion object {
        private const val KEY_DURATION = "AnimatorChangeHandler.duration"
        private const val KEY_REMOVES_FROM_ON_PUSH = "AnimatorChangeHandler.removesFromViewOnPush"

        const val DEFAULT_ANIMATION_DURATION = -1L
    }
}