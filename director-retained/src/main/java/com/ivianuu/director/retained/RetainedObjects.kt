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

package com.ivianuu.director.retained

import com.ivianuu.director.Controller
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Objects which are retained trough config changes
 * So every [Controller] will get the same instance back trough its lifetime
 */
class RetainedObjects {

    /**
     * All entries
     */
    val entries: Map<String, Any?> get() = _entries.toMap()
    private val _entries = mutableMapOf<String, Any?>()

    /**
     * Returns the value for [key] or null
     */
    operator fun <T> get(key: String): T? = _entries[key] as? T

    /**
     * Sets the value for [key] to [value]
     */
    operator fun <T> set(key: String, value: T) {
        _entries[key] = value
    }

    /**
     * Removes the value for [key]
     */
    fun <T> remove(key: String): T? = _entries.remove(key) as? T

    /**
     * Clears all values
     */
    fun clear() {
        _entries.clear()
    }

    /**
     * Whether or not contains a value for [key]
     */
    fun contains(key: String): Boolean = _entries.contains(key)
}

/**
 * Returns the value for [key] or sets the result of [defaultValue]
 */
fun <T> RetainedObjects.getOrSet(key: String, defaultValue: () -> T): T {
    var value = get<T>(key)

    if (value == null) {
        value = defaultValue()
        set(key, value)
    }

    return value as T
}

/**
 * Returns the value for [key] or the result of [defaultValue]
 */
fun <T> RetainedObjects.getOrDefault(key: String, defaultValue: () -> T): T {
    var value = get<T>(key)

    if (value == null) {
        value = defaultValue()
    }

    return value as T
}

/**
 * Returns the retained objects of this controller
 */
val Controller.retainedObjects: RetainedObjects
    get() = RetainedObjectsHolder.get(this)

private const val USE_PROPERTY_NAME = "RetainedObjects.usePropertyName"

fun <T> Controller.retained(
    key: String = USE_PROPERTY_NAME,
    initializer: () -> T
): ReadWriteProperty<Any, T> =
    RetainedProperty(this, key, initializer)

private class RetainedProperty<T>(
    private val controller: Controller,
    private val key: String,
    private val initializer: () -> T
) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val key = if (key == USE_PROPERTY_NAME) {
            property.name
        } else {
            this.key
        }

        return controller.retainedObjects.getOrSet(key, initializer)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val key = if (key == USE_PROPERTY_NAME) {
            property.name
        } else {
            this.key
        }

        controller.retainedObjects[key] = value
    }
}