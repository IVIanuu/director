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

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val USE_PROPERTY_NAME = "RetainedObjects.usePropertyName"

/**
 * Objects which are retained trough config changes
 * So every [Controller] will get the same instance back
 */
class RetainedObjects {

    val entries: Map<String, *> get() = _entries.toMap()
    private val _entries = mutableMapOf<String, Any>()

    operator fun <T : Any> get(key: String): T? = _entries[key] as? T

    fun <T : Any> getOrPut(key: String, defaultValue: () -> T): T =
        _entries.getOrPut(key, defaultValue) as T

    fun <T : Any> put(key: String, value: T) {
        set(key, value)
    }

    operator fun <T : Any> set(key: String, value: T) {
        _entries[key] = value
    }

    fun <T : Any> remove(key: String): T? = _entries.remove(key) as? T

    fun clear() {
        _entries.clear()
    }

    fun contains(key: String): Boolean = _entries.contains(key)
}

fun <T : Any> Controller.retainedLazy(
    key: String = USE_PROPERTY_NAME,
    initializer: () -> T
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { retainedObjects.getOrPut(key, initializer) }

fun <T : Any> retained(
    key: String = USE_PROPERTY_NAME,
    initialValue: () -> T
): ReadWriteProperty<Controller, T> = RetainedProperty(initialValue, key)

private class RetainedProperty<T : Any>(
    private val initialValue: () -> T,
    private val key: String
) : ReadWriteProperty<Controller, T> {

    private val usePropertyName get() = key == USE_PROPERTY_NAME

    override fun getValue(thisRef: Controller, property: KProperty<*>): T {
        val key = if (usePropertyName) {
            property.name
        } else {
            this.key
        }

        return thisRef.retainedObjects.getOrPut(key, initialValue)
    }

    override fun setValue(thisRef: Controller, property: KProperty<*>, value: T) {
        val key = if (usePropertyName) {
            property.name
        } else {
            this.key
        }

        thisRef.retainedObjects.put(key, value)
    }
}