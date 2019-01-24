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

private const val USE_PROPERTY_NAME = "RetainedObjects.usePropertyName"

/**
 * Objects which are retained trough config changes
 * So every [Controller] will get the same instance back
 */
class RetainedObjects {

    /**
     * All entries
     */
    val entries: Map<String, Any> get() = _entries.toMap()
    private val _entries = mutableMapOf<String, Any>()

    /**
     * Returns the value for [key] or null
     */
    operator fun <T> get(key: String): T? = _entries[key] as? T

    /**
     * Sets the value for [key] to [value]
     */
    fun <T> put(key: String, value: T) {
        set(key, value)
    }

    /**
     * Sets the value for [key] to [value]
     */
    operator fun <T> set(key: String, value: T) {
        _entries[key] = value as Any
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
 * Returns the value for [key] or puts the result of [defaultValue]
 */
fun <T> RetainedObjects.getOrPut(key: String, defaultValue: () -> T): T {
    var value = get<T>(key)

    if (value == null) {
        value = defaultValue()
        put(key, value)
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

fun <T> Controller.retainedLazy(
    key: String = com.ivianuu.director.retained.USE_PROPERTY_NAME,
    initializer: () -> T
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { retainedObjects.getOrPut(key, initializer) }

fun <T> retained(
    key: String = USE_PROPERTY_NAME,
    initialValue: () -> T
): ReadWriteProperty<Controller, T> =
    RetainedProperty(initialValue, key)

private class RetainedProperty<T>(
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