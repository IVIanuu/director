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
import com.ivianuu.director.ChangeData
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.DirectorPlugins
import com.ivianuu.director.common.OnReadyOrAbortedListener
import com.ivianuu.director.defaultRemovesFromViewOnPush

/**
 * A base [ControllerChangeHandler] that uses [android.animation.Animator]s to replace Controller Views
 */
abstract class AnimatorChangeHandler(
    duration: Long = DirectorPlugins.defaultAnimationDuration,
    override var removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
) : ControllerChangeHandler() {

    var duration = duration
        private set

    private var animator: Animator? = null
    private var onReadyOrAbortedListener: OnReadyOrAbortedListener? = null
    private var changeData: ChangeData? = null

    private var canceled = false
    private var completed = false

    override fun performChange(changeData: ChangeData) {
        this.changeData = changeData

        changeData.callback.addToView()

        if (changeData.to != null
            && changeData.to!!.width <= 0
            && changeData.to!!.height <= 0
        ) {
            onReadyOrAbortedListener = OnReadyOrAbortedListener(changeData.to!!) {
                performAnimation(changeData)
            }
        } else {
            performAnimation(changeData)
        }
    }

    override fun saveToBundle(outState: Bundle) {
        super.saveToBundle(outState)
        outState.putLong(KEY_DURATION, duration)
        outState.putBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH, removesFromViewOnPush)
    }

    override fun restoreFromBundle(savedInstanceState: Bundle) {
        super.restoreFromBundle(savedInstanceState)
        duration = savedInstanceState.getLong(KEY_DURATION)
        removesFromViewOnPush = savedInstanceState.getBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH)
    }

    override fun cancel() {
        super.cancel()
        canceled = true

        when {
            animator != null -> animator?.cancel()
            onReadyOrAbortedListener != null -> onReadyOrAbortedListener?.onReadyOrAborted()
            changeData != null -> complete()
        }
    }

    /**
     * Should be overridden to return the Animator to use while replacing Views.
     */
    protected abstract fun getAnimator(changeData: ChangeData): Animator

    /**
     * Called after the animation was finished this should reset the view to the pre anim state
     */
    protected open fun resetFromView(from: View) {
    }

    private fun performAnimation(changeData: ChangeData) {
        if (canceled) {
            complete()
            return
        }

        animator = getAnimator(changeData).apply {
            if (this@AnimatorChangeHandler.duration != NO_DURATION) {
                duration = this@AnimatorChangeHandler.duration
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    complete()
                }
            })
        }

        animator?.start()
    }

    private fun complete() {
        if (completed) return
        completed = true

        val (_, from, _, isPush,
            callback, _, _) = changeData!!

        callback.removeFromView()

        if (isPush && from != null) {
            resetFromView(from)
        }

        callback.changeCompleted()

        animator = null
        onReadyOrAbortedListener = null
        changeData = null
    }

    companion object {
        private const val KEY_DURATION = "AnimatorChangeHandler.duration"
        private const val KEY_REMOVES_FROM_VIEW_ON_PUSH =
            "AnimatorChangeHandler.removesFromViewOnPush"

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