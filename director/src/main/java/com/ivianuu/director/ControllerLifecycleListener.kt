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

/**
 * Lifecycle listener for [Controller]s
 */
interface ControllerLifecycleListener {

    fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
    }

    fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
    }

    fun preBuildView(controller: Controller, savedViewState: Bundle?) {
    }

    fun postBuildView(controller: Controller, view: View, savedViewState: Bundle?) {
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

    fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
    }

    fun onSaveInstanceState(controller: Controller, outState: Bundle) {
    }

    fun onRestoreViewState(controller: Controller, view: View, savedViewState: Bundle) {
    }

    fun onSaveViewState(controller: Controller, view: View, outState: Bundle) {
    }

    fun onChangeStart(
        controller: Controller,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
    }

    fun onChangeEnd(
        controller: Controller,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
    }
}

/**
 * Returns a new [ControllerLifecycleListener]
 */
fun ControllerLifecycleListener(
    preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    preBuildView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    postBuildView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
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
    onRestoreInstanceState: ((controller: Controller, savedInstanceState: Bundle) -> Unit)? = null,
    onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onRestoreViewState: ((controller: Controller, view: View, savedViewState: Bundle) -> Unit)? = null,
    onSaveViewState: ((controller: Controller, view: View, outState: Bundle) -> Unit)? = null,
    onChangeStart: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerLifecycleListener = LambdaLifecycleListener(
    preCreate = preCreate, postCreate = postCreate,
    preBuildView = preBuildView, postBuildView = postBuildView,
    preBindView = preBindView, postBindView = postBindView,
    preAttach = preAttach, postAttach = postAttach,
    preDetach = preDetach, postDetach = postDetach,
    preUnbindView = preUnbindView, postUnbindView = postUnbindView,
    preDestroy = preDestroy, postDestroy = postDestroy,
    onRestoreInstanceState = onRestoreInstanceState, onSaveInstanceState = onSaveInstanceState,
    onRestoreViewState = onRestoreViewState, onSaveViewState = onSaveViewState,
    onChangeStart = onChangeStart, onChangeEnd = onChangeEnd
)

fun Controller.doOnPreCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preCreate = block)

fun Controller.doOnPostCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postCreate = block)

fun Controller.doOnPreBuildView(block: (controller: Controller, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preBuildView = block)

fun Controller.doOnPostBuildView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postBuildView = block)

fun Controller.doOnPreBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preBindView = block)

fun Controller.doOnPostBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postBindView = block)

fun Controller.doOnPreAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preAttach = block)

fun Controller.doOnPostAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(postAttach = block)

fun Controller.doOnPreDetach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(preDetach = block)

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

fun Controller.doOnRestoreInstanceState(block: (controller: Controller, savedInstanceState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onRestoreInstanceState = block)

fun Controller.doOnSaveInstanceState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onSaveInstanceState = block)

fun Controller.doOnRestoreViewState(block: (controller: Controller, view: View, savedViewState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onRestoreViewState = block)

fun Controller.doOnSaveViewState(block: (controller: Controller, view: View, outState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onSaveViewState = block)

fun Controller.doOnChangeStart(block: (controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onChangeStart = block)

fun Controller.doOnChangeEnd(block: (controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListener =
    addLifecycleListener(onChangeEnd = block)

fun Controller.addLifecycleListener(
    preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    preBuildView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    postBuildView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
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
    onRestoreInstanceState: ((controller: Controller, savedInstanceState: Bundle) -> Unit)? = null,
    onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onRestoreViewState: ((controller: Controller, view: View, savedViewState: Bundle) -> Unit)? = null,
    onSaveViewState: ((controller: Controller, view: View, outState: Bundle) -> Unit)? = null,
    onChangeStart: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerLifecycleListener = ControllerLifecycleListener(
    preCreate = preCreate, postCreate = postCreate,
    preBuildView = preBuildView, postBuildView = postBuildView,
    preBindView = preBindView, postBindView = postBindView,
    preAttach = preAttach, postAttach = postAttach,
    preDetach = preDetach, postDetach = postDetach,
    preUnbindView = preUnbindView, postUnbindView = postUnbindView,
    preDestroy = preDestroy, postDestroy = postDestroy,
    onRestoreInstanceState = onRestoreInstanceState, onSaveInstanceState = onSaveInstanceState,
    onRestoreViewState = onRestoreViewState, onSaveViewState = onSaveViewState,
    onChangeStart = onChangeStart, onChangeEnd = onChangeEnd
).also { addLifecycleListener(it) }

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

fun Router.doOnControllerPreBuildView(
    recursive: Boolean = false,
    block: (controller: Controller, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, preBuildView = block)

fun Router.doOnControllerPostBuildView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, postBuildView = block)

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
    addLifecycleListener(recursive = recursive, preDetach = block)

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

fun Router.doOnControllerRestoreInstanceState(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onRestoreInstanceState = block)

fun Router.doOnControllerSaveInstanceState(
    recursive: Boolean = false,
    block: (controller: Controller, outState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onSaveInstanceState = block)

fun Router.doOnControllerRestoreViewState(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onRestoreViewState = block)

fun Router.doOnControllerSaveViewState(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, outState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onSaveViewState = block)

fun Router.doOnControllerChangeStart(
    recursive: Boolean = false,
    block: (controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onChangeStart = block)

fun Router.doOnControllerChangeEnd(
    recursive: Boolean = false,
    block: (controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive = recursive, onChangeEnd = block)

fun Router.addLifecycleListener(
    recursive: Boolean = false,
    preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    preBuildView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    postBuildView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
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
    onRestoreInstanceState: ((controller: Controller, savedInstanceState: Bundle) -> Unit)? = null,
    onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    onRestoreViewState: ((controller: Controller, view: View, savedViewState: Bundle) -> Unit)? = null,
    onSaveViewState: ((controller: Controller, view: View, outState: Bundle) -> Unit)? = null,
    onChangeStart: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerLifecycleListener {
    return ControllerLifecycleListener(
        preCreate = preCreate, postCreate = postCreate,
        preBuildView = preBuildView, postBuildView = postBuildView,
        preBindView = preBindView, postBindView = postBindView,
        preAttach = preAttach, postAttach = postAttach,
        preDetach = preDetach, postDetach = postDetach,
        preUnbindView = preUnbindView, postUnbindView = postUnbindView,
        preDestroy = preDestroy, postDestroy = postDestroy,
        onRestoreInstanceState = onRestoreInstanceState, onSaveInstanceState = onSaveInstanceState,
        onRestoreViewState = onRestoreViewState, onSaveViewState = onSaveViewState,
        onChangeStart = onChangeStart, onChangeEnd = onChangeEnd
    ).also { addLifecycleListener(it, recursive) }
}

private class LambdaLifecycleListener(
    private val preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    private val postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    private val preBuildView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    private val postBuildView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    private val preBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    private val postBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
    private val preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    private val postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    private val preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    private val postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    private val preUnbindView: ((controller: Controller, view: View) -> Unit)? = null,
    private val postUnbindView: ((controller: Controller) -> Unit)? = null,
    private val preDestroy: ((controller: Controller) -> Unit)? = null,
    private val postDestroy: ((controller: Controller) -> Unit)? = null,
    private val onRestoreInstanceState: ((controller: Controller, savedInstanceState: Bundle) -> Unit)? = null,
    private val onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    private val onRestoreViewState: ((controller: Controller, view: View, savedViewState: Bundle) -> Unit)? = null,
    private val onSaveViewState: ((controller: Controller, view: View, outState: Bundle) -> Unit)? = null,
    private val onChangeStart: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    private val onChangeEnd: ((controller: Controller, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
) : ControllerLifecycleListener {
    override fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
        preCreate?.invoke(controller, savedInstanceState)
    }

    override fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
        postCreate?.invoke(controller, savedInstanceState)
    }

    override fun preBuildView(controller: Controller, savedViewState: Bundle?) {
        preBuildView?.invoke(controller, savedViewState)
    }

    override fun postBuildView(controller: Controller, view: View, savedViewState: Bundle?) {
        postBuildView?.invoke(controller, view, savedViewState)
    }

    override fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
        preBindView?.invoke(controller, view, savedViewState)
    }

    override fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
        postBindView?.invoke(controller, view, savedViewState)
    }

    override fun preAttach(controller: Controller, view: View) {
        preAttach?.invoke(controller, view)
    }

    override fun postAttach(controller: Controller, view: View) {
        postAttach?.invoke(controller, view)
    }

    override fun preDetach(controller: Controller, view: View) {
        preDetach?.invoke(controller, view)
    }

    override fun postDetach(controller: Controller, view: View) {
        postDetach?.invoke(controller, view)
    }

    override fun preUnbindView(controller: Controller, view: View) {
        preUnbindView?.invoke(controller, view)
    }

    override fun postUnbindView(controller: Controller) {
        postUnbindView?.invoke(controller)
    }

    override fun preDestroy(controller: Controller) {
        preDestroy?.invoke(controller)
    }

    override fun postDestroy(controller: Controller) {
        postDestroy?.invoke(controller)
    }

    override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
        onRestoreInstanceState?.invoke(controller, savedInstanceState)
    }

    override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        onSaveInstanceState?.invoke(controller, outState)
    }

    override fun onRestoreViewState(controller: Controller, view: View, savedViewState: Bundle) {
        onRestoreViewState?.invoke(controller, view, savedViewState)
    }

    override fun onSaveViewState(controller: Controller, view: View, outState: Bundle) {
        onSaveViewState?.invoke(controller, view, outState)
    }

    override fun onChangeStart(
        controller: Controller,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeStart?.invoke(controller, changeHandler, changeType)
    }

    override fun onChangeEnd(
        controller: Controller,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeEnd?.invoke(controller, changeHandler, changeType)
    }
}