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

package com.ivianuu.director

import android.os.Bundle
import android.view.View
import android.view.ViewGroup

/**
 * A [ControllerChangeHandler] that will instantly swap Views with no animations or transitions.
 */
open class SimpleSwapChangeHandler(override var removesFromViewOnPush: Boolean = true) : ControllerChangeHandler() {

    private var container: ViewGroup? = null
    private var onChangeComplete: (() -> Unit)? = null

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            v.removeOnAttachStateChangeListener(this)

            onChangeComplete?.invoke()
            onChangeComplete = null
            container = null
        }

        override fun onViewDetachedFromWindow(v: View) {
        }
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putBoolean(KEY_REMOVES_FROM_ON_PUSH, removesFromViewOnPush)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        removesFromViewOnPush = bundle.getBoolean(KEY_REMOVES_FROM_ON_PUSH)
    }

    override fun cancel(immediate: Boolean) {
        super.cancel(immediate)
        onChangeComplete?.let {
            it()
            onChangeComplete = null
            container?.removeOnAttachStateChangeListener(attachStateChangeListener)
            container = null
        }
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
        this.onChangeComplete = onChangeComplete
        this.container = container

        if (from != null && (!isPush || removesFromViewOnPush)) {
            container.removeView(from)
        }

        if (to != null && to.parent == null) {
            container.addView(to)
        }

        if (container.windowToken != null) {
            onChangeComplete()
            this.container = null
            this.onChangeComplete = null
        } else {
            container.addOnAttachStateChangeListener(attachStateChangeListener)
        }
    }

    override fun copy(): SimpleSwapChangeHandler = SimpleSwapChangeHandler(removesFromViewOnPush)

    companion object {
        private const val KEY_REMOVES_FROM_ON_PUSH = "SimpleSwapChangeHandler.removesFromViewOnPush"
    }
}

fun RouterTransactionBuilder.simpleSwap(
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    handler(SimpleSwapChangeHandler(removesFromViewOnPush))
}

fun RouterTransactionBuilder.simpleSwapPush(
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    pushHandler(SimpleSwapChangeHandler(removesFromViewOnPush))
}

fun RouterTransactionBuilder.simpleSwapPop(
    removesFromViewOnPush: Boolean = true
): RouterTransactionBuilder = apply {
    popHandler(SimpleSwapChangeHandler(removesFromViewOnPush))
}
