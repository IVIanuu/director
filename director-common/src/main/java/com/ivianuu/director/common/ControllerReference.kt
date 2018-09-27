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

import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property that auto clears a value on a specified time
 * This is useful for stuff like dependency injection
 */
class ControllerRef<T : Any>(controller: Controller, mode: Mode) : ReadWriteProperty<Controller, T> {

    private var value: T? = null

    init {
        controller.addLifecycleListener(object : ControllerLifecycleListener {
            override fun postDestroyView(controller: Controller) {
                if (mode == Mode.VIEW) {
                    value = null
                }
            }

            override fun postContextUnavailable(controller: Controller) {
                super.postContextUnavailable(controller)
                if (mode == Mode.CONTEXT) {
                    value = null
                }
            }
        })
    }

    override fun getValue(thisRef: Controller, property: KProperty<*>) = value
        ?: throw IllegalStateException("Property ${property.name} should be initialized before get and not called after postDestroyView")

    override fun setValue(thisRef: Controller, property: KProperty<*>, value: T) {
        this.value = value
    }

    enum class Mode {
        CONTEXT, VIEW
    }
}

/**
 * Clears the value based on the mode
 */
fun <T : Any> Controller.controllerRef(mode: ControllerRef.Mode): ReadWriteProperty<Controller, T> = ControllerRef(this, mode)

/**
 * Clears the value in [Controller.onContextUnavailable]
 */
fun <T : Any> Controller.contextRef(): ReadWriteProperty<Controller, T> = controllerRef(ControllerRef.Mode.CONTEXT)

/**
 * Clears the value in [Controller.onDestroyView]
 */
fun <T : Any> Controller.viewRef(): ReadWriteProperty<Controller, T> = controllerRef(ControllerRef.Mode.VIEW)