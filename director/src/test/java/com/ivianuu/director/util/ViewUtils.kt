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
import org.robolectric.util.ReflectionHelpers

object ViewUtils {

    @JvmOverloads fun reportAttached(
        view: View,
        attached: Boolean,
        propogateToChildren: Boolean = true
    ) {
        if (view is AttachFakingFrameLayout) {
            view.setAttached(attached, false)
        }

        val listeners = getAttachStateListeners(view)

        for (listener in listeners) {
            if (attached) {
                listener.onViewAttachedToWindow(view)
            } else {
                listener.onViewDetachedFromWindow(view)
            }
        }

        if (propogateToChildren && view is ViewGroup) {
            val childCount = view.childCount
            for (i in 0 until childCount) {
                reportAttached(view.getChildAt(i), attached, true)
            }
        }
    }

    private fun getAttachStateListeners(view: View): List<OnAttachStateChangeListener> {
        val listenerInfo = ReflectionHelpers.callInstanceMethod<Any>(view, "getListenerInfo")
        return ReflectionHelpers.getField(listenerInfo, "mOnAttachStateChangeListeners")
            ?: emptyList()
    }
}