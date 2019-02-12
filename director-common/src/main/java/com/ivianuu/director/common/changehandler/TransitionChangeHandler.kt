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
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.DirectorPlugins

/**
 * A base [ControllerChangeHandler] that facilitates using [android.transition.Transition]s to replace Controller Views.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
abstract class TransitionChangeHandler(
    duration: Long = DirectorPlugins.defaultTransitionDuration
) : ControllerChangeHandler() {

    var duration = duration
        private set

    private var canceled = false
    private var needsImmediateCompletion = false

    override val removesFromViewOnPush: Boolean get() = true

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
        if (canceled) {
            if (needsImmediateCompletion) {
                executePropertyChanges(container, from, to, toIndex, null, isPush)
            }
            onChangeComplete()
            return
        }

        val transition = getTransition(container, from, to, toIndex, isPush)
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
                onChangeComplete()
            }

            override fun onTransitionEnd(transition: Transition) {
                onChangeComplete()
            }
        })

        prepareForTransition(
            container,
            from,
            to,
            toIndex,
            transition,
            isPush
        ) {
            // todo transition manager needs a fully attached container
            if (container.isLaidOut) {
                TransitionManager.beginDelayedTransition(container, transition)
                executePropertyChanges(container, from, to, toIndex, transition, isPush)
            } else {
                executePropertyChanges(container, from, to, toIndex, transition, isPush)
                onChangeComplete()
            }
        }
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putLong(KEY_DURATION, duration)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        duration = bundle.getLong(KEY_DURATION)
    }

    override fun cancel(immediate: Boolean) {
        super.cancel(immediate)
        canceled = true
        needsImmediateCompletion = immediate
    }

    /**
     * Should be overridden to return the Transition to use while replacing Views.
     */
    protected abstract fun getTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        isPush: Boolean
    ): Transition

    /**
     * Called before a transition occurs. This can be used to reorder views, set their transition names, etc. The transition will begin
     * when `onTransitionPreparedListener` is called.
     */
    protected open fun prepareForTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        transition: Transition,
        isPush: Boolean,
        onTransitionPrepared: () -> Unit
    ) {
        onTransitionPrepared()
    }

    /**
     * This should set all view properties needed for the transition to work properly. By default it removes the "from" view
     * and adds the "to" view.
     */
    protected open fun executePropertyChanges(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        transition: Transition?,
        isPush: Boolean
    ) {
        if (from != null && (removesFromViewOnPush || !isPush) && from.parent == container) {
            container.removeView(from)
        }
        if (to != null && to.parent == null) {
            container.addView(to, toIndex)
        }
    }

    companion object {
        private const val KEY_DURATION = "TransitionChangeHandler.duration"
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