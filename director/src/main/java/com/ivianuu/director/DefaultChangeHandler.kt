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
import com.ivianuu.director.internal.moveView

/**
 * A [ChangeHandler] that will instantly swap Views with no animations or transitions.
 */
open class DefaultChangeHandler(
    removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
) : ChangeHandler() {

    override val removesFromViewOnPush: Boolean get() = _removesFromViewOnPush
    private var _removesFromViewOnPush = removesFromViewOnPush

    private var changeData: ChangeData? = null

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            v.removeOnAttachStateChangeListener(this)
            changeData?.onChangeComplete?.invoke()
            changeData = null
        }

        override fun onViewDetachedFromWindow(v: View) {
        }
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH, _removesFromViewOnPush)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        _removesFromViewOnPush = bundle.getBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH)
    }

    override fun cancel() {
        super.cancel()
        changeData?.let {
            it.onChangeComplete()
            it.container.removeOnAttachStateChangeListener(attachStateChangeListener)
        }
        changeData = null
    }

    override fun performChange(changeData: ChangeData) {
        this.changeData = changeData

        val (container, from, to, isPush,
            onChangeComplete, toIndex, forceRemoveFromViewOnPush) = changeData

        if (to != null) {
            if (to.parent == null) {
                container.addView(to, toIndex)
            } else if (container.indexOfChild(to) != toIndex) {
                container.moveView(to, toIndex)
            }
        }

        if (from != null && (!isPush || removesFromViewOnPush || forceRemoveFromViewOnPush)) {
            container.removeView(from)
        }

        if (container.windowToken != null) {
            onChangeComplete()
            this.changeData = null
        } else {
            container.addOnAttachStateChangeListener(attachStateChangeListener)
        }
    }

    override fun copy(): DefaultChangeHandler = DefaultChangeHandler(removesFromViewOnPush)

    companion object {
        private const val KEY_REMOVES_FROM_VIEW_ON_PUSH =
            "DefaultChangeHandler.removesFromViewOnPush"
    }
}