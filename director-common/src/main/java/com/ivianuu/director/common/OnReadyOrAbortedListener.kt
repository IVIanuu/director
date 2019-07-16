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

import android.view.View
import android.view.ViewTreeObserver

class OnReadyOrAbortedListener(
    val view: View,
    val action: () -> Unit
) : ViewTreeObserver.OnPreDrawListener {

    private var finished = false

    init {
        view.viewTreeObserver.addOnPreDrawListener(this)
    }

    override fun onPreDraw(): Boolean {
        onReadyOrAborted()
        return true
    }

    fun onReadyOrAborted() {
        if (finished) return
        finished = true

        val observer = view.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnPreDrawListener(this)
        }

        action()
    }
}