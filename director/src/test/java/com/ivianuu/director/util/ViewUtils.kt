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

package com.ivianuu.director.util

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewParent
import org.robolectric.util.ReflectionHelpers

fun View.reportAttached(attached: Boolean, applyOnChildren: Boolean = true) {
    if (this is AttachFakingFrameLayout) {
        setAttached(attached, false)
    }

    getAttachStateListeners().forEach {
        if (attached) {
            it.onViewAttachedToWindow(this)
        } else {
            it.onViewDetachedFromWindow(this)
        }
    }

    if (applyOnChildren && this is ViewGroup) {
        (0 until childCount)
            .map(this::getChildAt)
            .forEach { it.reportAttached(attached, true) }
    }
}

fun View.setParent(parent: ViewParent?) {
    ReflectionHelpers.setField(this, "mParent", parent)
}

private fun View.getAttachStateListeners(): List<OnAttachStateChangeListener> {
    val listenerInfo = ReflectionHelpers.callInstanceMethod<Any>(this, "getListenerInfo")
    return ReflectionHelpers.getField(listenerInfo, "mOnAttachStateChangeListeners")
        ?: emptyList()
}