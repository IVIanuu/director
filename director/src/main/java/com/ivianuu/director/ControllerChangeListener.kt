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
     * Called when a [ChangeHandler] has started changing [Controller]s
     */
    fun onChangeStarted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler
    ) {
    }

    /**
     * Called when a [ChangeHandler] has completed changing [Controller]s
     */
    fun onChangeCompleted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler
    ) {
    }

}

/**
 * Returns a [ControllerChangeListener]
 */
fun ControllerChangeListener(
    onChangeStarted: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null,
    onChangeEnded: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null
): ControllerChangeListener =
    LambdaControllerChangeListener(onChangeStarted, onChangeEnded)

fun Router.doOnChangeStarted(
    recursive: Boolean = false,
    block: (to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit
): ControllerChangeListener =
    addChangeListener(recursive = recursive, onChangeStarted = block)

fun Router.doOnChangeEnded(
    recursive: Boolean = false,
    block: (to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit
): ControllerChangeListener =
    addChangeListener(recursive = recursive, onChangeEnded = block)

fun Router.addChangeListener(
    recursive: Boolean = false,
    onChangeStarted: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null,
    onChangeEnded: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null
): ControllerChangeListener =
    ControllerChangeListener(onChangeStarted, onChangeEnded).also {
        addChangeListener(
            it,
            recursive
        )
    }

private class LambdaControllerChangeListener(
    private val onChangeStarted: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null,
    private val onChangeEnded: ((to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null
) : ControllerChangeListener {
    override fun onChangeStarted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler
    ) {
        onChangeStarted?.invoke(to, from, isPush, container, handler)
    }

    override fun onChangeCompleted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler
    ) {
        onChangeEnded?.invoke(to, from, isPush, container, handler)
    }
}