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

package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup
import java.util.*

fun <T : Any> newInstanceOrThrow(className: String): T = try {
    classForNameOrThrow(className).newInstance() as T
} catch (e: Exception) {
    throw RuntimeException("couldn't instantiate $className, $e")
}

fun classForNameOrThrow(className: String): Class<*> = try {
    Class.forName(className)
} catch (e: Exception) {
    throw RuntimeException("couldn't find class $className")
}

fun ViewGroup.moveView(view: View, to: Int) {
    if (to == -1) {
        view.bringToFront()
        return
    }
    val index = indexOfChild(view)
    if (index == -1 || index == to) return
    val allViews = (0 until childCount).map { getChildAt(it) }.toMutableList()
    Collections.swap(allViews, index, to)
    allViews.forEach { it.bringToFront() }
    requestLayout()
    invalidate()
}