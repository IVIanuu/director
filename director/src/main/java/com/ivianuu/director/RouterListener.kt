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
interface RouterListener {

    /**
     * Called when a [ChangeHandler] has started changing [Controller]s
     */
    fun onChangeStarted(
        router: Router,
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
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler
    ) {
    }

}

/**
 * Returns a [RouterListener]
 */
fun RouterListener(
    onChangeStarted: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null,
    onChangeEnded: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null
): RouterListener =
    LambdaRouterListener(onChangeStarted, onChangeEnded)

fun Router.doOnChangeStarted(
    recursive: Boolean = false,
    block: (router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit
): RouterListener =
    addListener(recursive = recursive, onChangeStarted = block)

fun Router.doOnChangeEnded(
    recursive: Boolean = false,
    block: (router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit
): RouterListener =
    addListener(recursive = recursive, onChangeEnded = block)

fun Router.addListener(
    recursive: Boolean = false,
    onChangeStarted: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null,
    onChangeEnded: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null
): RouterListener =
    RouterListener(onChangeStarted, onChangeEnded).also {
        addListener(
            it,
            recursive
        )
    }

private class LambdaRouterListener(
    private val onChangeStarted: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null,
    private val onChangeEnded: ((router: Router, to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ChangeHandler) -> Unit)? = null
) : RouterListener {
    override fun onChangeStarted(
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler
    ) {
        onChangeStarted?.invoke(router, to, from, isPush, container, handler)
    }

    override fun onChangeCompleted(
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ChangeHandler
    ) {
        onChangeEnded?.invoke(router, to, from, isPush, container, handler)
    }
}