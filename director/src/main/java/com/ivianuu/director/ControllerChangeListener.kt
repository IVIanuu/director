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

import android.view.ViewGroup

/**
 * A listener interface useful for allowing external classes to be notified of change events.
 */
interface ControllerChangeListener {
    /**
     * Called when a [ControllerChangeHandler] has started changing [Controller]s
     */
    fun onChangeStarted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
    }

    /**
     * Called when a [ControllerChangeHandler] has completed changing [Controller]s
     */
    fun onChangeCompleted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
    }
}

class ControllerChangeListenerBuilder internal constructor() {

    private var onChangeStarted: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? =
        null
    private var onChangeEnded: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? =
        null

    fun onChangeStarted(block: (to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit): ControllerChangeListenerBuilder =
        apply {
            this.onChangeStarted = block
        }

    fun onChangeEnded(block: (to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit): ControllerChangeListenerBuilder =
        apply {
            this.onChangeEnded = block
        }

    fun build(): ControllerChangeListener = LambdaControllerChangeListener(
        onChangeStarted = onChangeStarted, onChangeEnded = onChangeEnded
    )

}

/**
 * Returns a [controllerChangeListener] build by [init]
 */
fun controllerChangeListener(init: ControllerChangeListenerBuilder.() -> Unit): ControllerChangeListener =
    ControllerChangeListenerBuilder().apply(init).build()

fun Router.doOnChangeStarted(
    recursive: Boolean = false,
    block: (to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit
): ControllerChangeListener =
    addChangeListener(recursive) { onChangeStarted(block) }

fun Router.doOnChangeEnded(
    recursive: Boolean = false,
    block: (to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit
): ControllerChangeListener =
    addChangeListener(recursive) { onChangeEnded(block) }

fun Router.addChangeListener(
    recursive: Boolean = false,
    init: ControllerChangeListenerBuilder.() -> Unit
): ControllerChangeListener =
    controllerChangeListener(init).also { addChangeListener(it, recursive) }

private class LambdaControllerChangeListener(
    private val onChangeStarted: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null,
    private val onChangeEnded: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null
) : ControllerChangeListener {
    override fun onChangeStarted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        onChangeStarted?.invoke(to, from, isPush, container, handler)
    }

    override fun onChangeCompleted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        onChangeEnded?.invoke(to, from, isPush, container, handler)
    }
}