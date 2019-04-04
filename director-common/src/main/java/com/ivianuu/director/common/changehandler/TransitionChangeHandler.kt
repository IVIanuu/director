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

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionManager
import com.ivianuu.director.ChangeData
import com.ivianuu.director.ChangeHandler
import com.ivianuu.director.DirectorPlugins
import com.ivianuu.director.defaultRemovesFromViewOnPush

/**
 * A base [ChangeHandler] that facilitates using [android.transition.Transition]s to replace Controller Views.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
abstract class TransitionChangeHandler(
    duration: Long = DirectorPlugins.defaultTransitionDuration,
    override var removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
) : ChangeHandler() {

    var duration = duration
        private set

    private var canceled = false

    override fun performChange(changeData: ChangeData) {
        val (container, _, _, _,
            callback) = changeData

        val transition = getTransition(changeData)
        if (duration != NO_DURATION) {
            transition.duration = duration
        }

        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
            }

            override fun onTransitionResume(transition: Transition) {
            }

            override fun onTransitionPause(transition: Transition) {
            }

            override fun onTransitionCancel(transition: Transition) {
                callback.onChangeCompleted()
            }

            override fun onTransitionEnd(transition: Transition) {
                callback.onChangeCompleted()
            }
        })

        prepareForTransition(changeData, transition) {
            // todo transition manager needs a fully attached container
            if (container.isLaidOut) {
                TransitionManager.beginDelayedTransition(container, transition)
                executePropertyChanges(changeData, transition)
            } else {
                executePropertyChanges(changeData, transition)
                callback.onChangeCompleted()
            }
        }
    }

    override fun saveInstanceState(outState: Bundle) {
        super.saveInstanceState(outState)
        outState.putLong(KEY_DURATION, duration)
        outState.putBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH, removesFromViewOnPush)
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        super.restoreInstanceState(savedInstanceState)
        duration = savedInstanceState.getLong(KEY_DURATION)
        removesFromViewOnPush = savedInstanceState.getBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH)
    }

    override fun cancel() {
        super.cancel()
        canceled = true
    }

    /**
     * Returns the [Transition] to use to swap views
     */
    protected abstract fun getTransition(changeData: ChangeData): Transition

    /**
     * Called before starting the [transition]
     */
    protected open fun prepareForTransition(
        changeData: ChangeData,
        transition: Transition,
        onTransitionPrepared: () -> Unit
    ) {
        onTransitionPrepared()
    }

    /**
     * This should set all view properties needed for the transition to work properly.
     * By default it removes the [from] view
     * and adds the [to] view.
     */
    protected open fun executePropertyChanges(
        changeData: ChangeData,
        transition: Transition?
    ) {
        changeData.callback.addToView()
        changeData.callback.removeFromView()
    }

    companion object {
        private const val KEY_DURATION = "TransitionChangeHandler.duration"
        private const val KEY_REMOVES_FROM_VIEW_ON_PUSH =
            "TransitionChangeHandler.removesFromViewOnPush"
        const val NO_DURATION = -1L
    }
}


private var _defaultTransitionDuration = TransitionChangeHandler.NO_DURATION

/**
 * The default transition duration to use in all [TransitionChangeHandler]s
 */
var DirectorPlugins.defaultTransitionDuration: Long
    get() = _defaultTransitionDuration
    set(value) {
        _defaultTransitionDuration = value
    }