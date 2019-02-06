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

import android.os.Bundle
import android.view.View
import com.ivianuu.director.internal.LambdaLifecycleListener

/**
 * Allows external classes to listen for lifecycle events of a [Controller]
 */
interface ControllerLifecycleListener {

    fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
    }

    fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
    }

    fun preInflateView(controller: Controller, savedViewState: Bundle?) {
    }

    fun postInflateView(controller: Controller, view: View, savedViewState: Bundle?) {
    }

    fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
    }

    fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
    }

    fun preAttach(controller: Controller, view: View) {
    }

    fun postAttach(controller: Controller, view: View) {
    }

    fun preDetach(controller: Controller, view: View) {
    }

    fun postDetach(controller: Controller, view: View) {
    }

    fun preUnbindView(controller: Controller, view: View) {
    }

    fun postUnbindView(controller: Controller) {
    }

    fun preDestroy(controller: Controller) {
    }

    fun postDestroy(controller: Controller) {
    }

    fun onSaveInstanceState(controller: Controller, outState: Bundle) {
    }

    fun onSaveViewState(controller: Controller, outState: Bundle) {
    }

    fun onChangeStart(
        controller: Controller,
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
    }

    fun onChangeEnd(
        controller: Controller,
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
    }
}

fun Controller.doOnPreCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preCreate = block)

fun Controller.doOnPostCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postCreate = block)

fun Controller.doOnPreInflateView(block: (controller: Controller, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preInflateView = block)

fun Controller.doOnPostInflateView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postInflateView = block)

fun Controller.doOnPreBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preBindView = block)

fun Controller.doOnPostBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postBindView = block)

fun Controller.doOnPreAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preAttach = block)

fun Controller.doOnPostAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postAttach = block)

fun Controller.doOnPreDetach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postDetach = block)

fun Controller.doOnPostDetach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postDetach = block)

fun Controller.doOnPreUnbindView(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preUnbindView = block)

fun Controller.doOnPostUnbindView(block: (controller: Controller) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postUnbindView = block)

fun Controller.doOnPreDestroy(block: (controller: Controller) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preDestroy = block)

fun Controller.doOnPostDestroy(block: (controller: Controller) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postDestroy = block)

fun Controller.doOnSaveInstanceState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onSaveInstanceState = block)

fun Controller.doOnSaveViewState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onSaveViewState = block)

fun Controller.doOnChangeStart(block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onChangeStart = block)

fun Controller.doOnChangeEnd(block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onChangeEnd = block)

fun Controller.addLifecycleListener(
    preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    preInflateView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    postInflateView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    preBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    postBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    preUnbindView: ((controller: Controller, view: View) -> Unit)? = null,
    postUnbindView: ((controller: Controller) -> Unit)? = null,
    preDestroy: ((controller: Controller) -> Unit)? = null,
    postDestroy: ((controller: Controller) -> Unit)? = null,
    onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onSaveViewState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onChangeStart: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerLifecycleListener {
    return LambdaLifecycleListener(
        preCreate = preCreate, postCreate = postCreate,
        preInflateView = preInflateView, postInflateView = postInflateView,
        preBindView = preBindView, postBindView = postBindView,
        preAttach = preAttach, postAttach = postAttach,
        preDetach = preDetach, postDetach = postDetach,
        preUnbindView = preUnbindView, postUnbindView = postUnbindView,
        preDestroy = preDestroy, postDestroy = postDestroy,
        onSaveInstanceState = onSaveInstanceState,
        onSaveViewState = onSaveViewState,
        onChangeStart = onChangeStart, onChangeEnd = onChangeEnd
    ).also { addLifecycleListener(it) }
}

fun Router.doOnControllerPreCreate(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, preCreate = block)

fun Router.doOnControllerPostCreate(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postCreate = block)

fun Router.doOnControllerPreInflateView(
    recursive: Boolean = false,
    block: (controller: Controller, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, preInflateView = block)

fun Router.doOnControllerPostInflateView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postInflateView = block)

fun Router.doOnControllerPreBindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, preBindView = block)

fun Router.doOnControllerPostBindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postBindView = block)

fun Router.doOnControllerPreAttach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, preAttach = block)

fun Router.doOnControllerPostAttach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postAttach = block)

fun Router.doOnControllerPreDetach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postDetach = block)

fun Router.doOnControllerPostDetach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postDetach = block)

fun Router.doOnControllerPreUnbindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, preUnbindView = block)

fun Router.doOnControllerPostUnbindView(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postUnbindView = block)

fun Router.doOnControllerPreDestroy(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, preDestroy = block)

fun Router.doOnControllerPostDestroy(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postDestroy = block)

fun Router.doOnControllerSaveInstanceState(
    recursive: Boolean = false,
    block: (controller: Controller, outState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onSaveInstanceState = block)

fun Router.doOnControllerSaveViewState(
    recursive: Boolean = false,
    block: (controller: Controller, outState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onSaveViewState = block)

fun Router.doOnControllerChangeStart(
    recursive: Boolean = false,
    block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onChangeStart = block)

fun Router.doOnControllerChangeEnd(
    recursive: Boolean = false,
    block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onChangeEnd = block)

fun Router.addLifecycleListener(
    recursive: Boolean = false,
    preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    preInflateView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    postInflateView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    preBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    postBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    preUnbindView: ((controller: Controller, view: View) -> Unit)? = null,
    postUnbindView: ((controller: Controller) -> Unit)? = null,
    preDestroy: ((controller: Controller) -> Unit)? = null,
    postDestroy: ((controller: Controller) -> Unit)? = null,
    onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onSaveViewState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onChangeStart: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerLifecycleListener {
    return LambdaLifecycleListener(
        preCreate = preCreate, postCreate = postCreate,
        preInflateView = preInflateView, postInflateView = postInflateView,
        preBindView = preBindView, postBindView = postBindView,
        preAttach = preAttach, postAttach = postAttach,
        preDetach = preDetach, postDetach = postDetach,
        preUnbindView = preUnbindView, postUnbindView = postUnbindView,
        preDestroy = preDestroy, postDestroy = postDestroy,
        onSaveInstanceState = onSaveInstanceState,
        onSaveViewState = onSaveViewState,
        onChangeStart = onChangeStart, onChangeEnd = onChangeEnd
    ).also { addLifecycleListener(it, recursive) }
}