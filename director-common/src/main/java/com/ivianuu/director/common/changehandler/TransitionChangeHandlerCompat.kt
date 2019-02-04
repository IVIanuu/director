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

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.internal.newInstanceOrThrow

/**
 * A base [ControllerChangeHandler] that facilitates using [android.transition.Transition]s to replace Controller Views.
 * If the target device is running on a version of Android that doesn't support transitions, a fallback [ControllerChangeHandler] will be used.
 */
open class TransitionChangeHandlerCompat : ControllerChangeHandler {

    override val removesFromViewOnPush: Boolean
        get() = changeHandler.removesFromViewOnPush

    override var forceRemoveViewOnPush: Boolean
        get() = changeHandler.forceRemoveViewOnPush
        set(value) { changeHandler.forceRemoveViewOnPush = value }

    private lateinit var changeHandler: ControllerChangeHandler

    constructor()

    /**
     * Constructor that takes a [TransitionChangeHandler] for use with compatible devices, as well as a fallback
     * [ControllerChangeHandler] for use with older devices.
     */
    constructor(
        transitionChangeHandler: TransitionChangeHandler,
        fallbackChangeHandler: ControllerChangeHandler
    ) {
        changeHandler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            transitionChangeHandler
        } else {
            fallbackChangeHandler
        }
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
        changeHandler.performChange(container, from, to, isPush, onChangeComplete)
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putString(KEY_CHANGE_HANDLER_CLASS, changeHandler.javaClass.name)

        val stateBundle = Bundle()
        changeHandler.saveToBundle(stateBundle)
        bundle.putBundle(KEY_HANDLER_STATE, stateBundle)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        val className = bundle.getString(KEY_CHANGE_HANDLER_CLASS)!!
        changeHandler = newInstanceOrThrow(className)
        changeHandler.restoreFromBundle(bundle.getBundle(KEY_HANDLER_STATE)!!)
    }

    override fun copy(): TransitionChangeHandlerCompat = TransitionChangeHandlerCompat()
        .also { it.changeHandler = changeHandler.copy() }

    override fun cancel(immediate: Boolean) {
        super.cancel(immediate)
        changeHandler.cancel(immediate)
    }

    companion object {
        private const val KEY_CHANGE_HANDLER_CLASS = "TransitionChangeHandlerCompat.changeHandler.class"
        private const val KEY_HANDLER_STATE = "TransitionChangeHandlerCompat.changeHandler.state"
    }
}