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
 * Listener for controller changes
 */
interface ControllerChangeListener {

    /**
     * Called when a [ControllerChangeHandler] has started changing [Controller]s
     */
    fun onChangeStarted(
        router: Router,
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
    fun onChangeEnded(
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
    }

}

/**
 * Returns a [ControllerChangeListener]
 */
fun ControllerChangeListener(
    onChangeStarted: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null,
    onChangeEnded: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null
): ControllerChangeListener =
    LambdaControllerChangeListener(onChangeStarted, onChangeEnded)

fun Router.doOnChangeStarted(
    recursive: Boolean = false,
    block: (router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit
) = addChangeListener(recursive = recursive, onChangeStarted = block)

fun Router.doOnChangeEnded(
    recursive: Boolean = false,
    block: (router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit
) =
    addChangeListener(recursive = recursive, onChangeEnded = block)

fun Router.addChangeListener(
    recursive: Boolean = false,
    onChangeStarted: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null,
    onChangeEnded: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null
): ControllerChangeListener {
    val listener = ControllerChangeListener(onChangeStarted, onChangeEnded)
    addChangeListener(listener, recursive)
    return listener
}

private class LambdaControllerChangeListener(
    private val onChangeStarted: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null,
    private val onChangeEnded: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) -> Unit)? = null
) : ControllerChangeListener {
    override fun onChangeStarted(
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        onChangeStarted?.invoke(router, to, from, isPush, container, handler)
    }

    override fun onChangeEnded(
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        onChangeEnded?.invoke(router, to, from, isPush, container, handler)
    }
}