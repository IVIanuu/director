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

class ChangeHandlerHistory {

    private val entries = mutableListOf<Entry>()
    var isValidHistory = true

    fun addEntry(from: View?, to: View?, isPush: Boolean, handler: MockChangeHandler) {
        entries.add(Entry(from, to, isPush, handler))
    }

    fun size(): Int = entries.size

    fun fromViewAt(index: Int): View? = entries[index].from

    fun toViewAt(index: Int): View? = entries[index].to

    fun isPushAt(index: Int): Boolean = entries[index].isPush

    fun changeHandlerAt(index: Int): MockChangeHandler = entries[index].changeHandler

    fun latestFromView(): View? = fromViewAt(size() - 1)

    fun latestToView(): View? = toViewAt(size() - 1)

    fun latestIsPush(): Boolean = isPushAt(size() - 1)

    fun latestChangeHandler(): MockChangeHandler = changeHandlerAt(size() - 1)

    private class Entry(
        val from: View?,
        val to: View?,
        val isPush: Boolean,
        val changeHandler: MockChangeHandler
    )
}