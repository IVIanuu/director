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

import com.ivianuu.director.internal.newInstanceOrThrow

/**
 * A factory for [Controller]s
 */
interface ControllerFactory {

    /**
     * Returns a new instance of a [Controller] for [className]
     */
    fun createController(
        classLoader: ClassLoader,
        className: String
    ): Controller

}

/**
 * Controller factory which uses reflection to instantiate controllers
 */
class ReflectiveControllerFactory : ControllerFactory {
    override fun createController(classLoader: ClassLoader, className: String): Controller =
        newInstanceOrThrow(className)
}

/**
 * Returns a new instance of a [T]
 */
inline fun <reified T> ControllerFactory.createController(): T =
    createController(T::class.java.classLoader!!, T::class.java.name) as T