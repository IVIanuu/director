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

internal class ListenersHolder<T>(var parent: ListenersHolder<T>? = null) {

    private val listeners =
        mutableListOf<ListenerEntry<T>>()

    fun add(listener: T, recursive: Boolean = false) {
        listeners.add(ListenerEntry(listener, recursive))
    }

    fun remove(listener: T) {
        listeners.removeAll { it.listener == listener }
    }

    fun get(recursiveOnly: Boolean = false): List<T> {
        return listeners
            .filter { !recursiveOnly || it.recursive }
            .map(ListenerEntry<T>::listener) + (parent?.get(true) ?: emptyList())
    }

    data class ListenerEntry<T>(
        val listener: T,
        val recursive: Boolean
    )

}