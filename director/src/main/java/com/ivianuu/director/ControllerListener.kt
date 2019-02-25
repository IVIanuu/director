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
 * Listener for [Controller]s
 */
interface ControllerListener {

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
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
    }

    fun onChangeEnd(
        controller: Controller,
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
    }
}

/**
 * Returns a new [ControllerListener]
 */
fun ControllerListener(
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
    onChangeStart: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerListener = LambdaControllerListener(
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

fun Controller.doOnPreCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerListener =
    addListener(preCreate = block)

fun Controller.doOnPostCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerListener =
    addListener(postCreate = block)

fun Controller.doOnPreBuildView(block: (controller: Controller, savedViewState: Bundle?) -> Unit): ControllerListener =
    addListener(preBuildView = block)

fun Controller.doOnPostBuildView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerListener =
    addListener(postBuildView = block)

fun Controller.doOnPreBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerListener =
    addListener(preBindView = block)

fun Controller.doOnPostBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerListener =
    addListener(postBindView = block)

fun Controller.doOnPreAttach(block: (controller: Controller, view: View) -> Unit): ControllerListener =
    addListener(preAttach = block)

fun Controller.doOnPostAttach(block: (controller: Controller, view: View) -> Unit): ControllerListener =
    addListener(postAttach = block)

fun Controller.doOnPreDetach(block: (controller: Controller, view: View) -> Unit): ControllerListener =
    addListener(preDetach = block)

fun Controller.doOnPostDetach(block: (controller: Controller, view: View) -> Unit): ControllerListener =
    addListener(postDetach = block)

fun Controller.doOnPreUnbindView(block: (controller: Controller, view: View) -> Unit): ControllerListener =
    addListener(preUnbindView = block)

fun Controller.doOnPostUnbindView(block: (controller: Controller) -> Unit): ControllerListener =
    addListener(postUnbindView = block)

fun Controller.doOnPreDestroy(block: (controller: Controller) -> Unit): ControllerListener =
    addListener(preDestroy = block)

fun Controller.doOnPostDestroy(block: (controller: Controller) -> Unit): ControllerListener =
    addListener(postDestroy = block)

fun Controller.doOnRestoreInstanceState(block: (controller: Controller, savedInstanceState: Bundle) -> Unit): ControllerListener =
    addListener(onRestoreInstanceState = block)

fun Controller.doOnSaveInstanceState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerListener =
    addListener(onSaveInstanceState = block)

fun Controller.doOnRestoreViewState(block: (controller: Controller, view: View, savedViewState: Bundle) -> Unit): ControllerListener =
    addListener(onRestoreViewState = block)

fun Controller.doOnSaveViewState(block: (controller: Controller, view: View, outState: Bundle) -> Unit): ControllerListener =
    addListener(onSaveViewState = block)

fun Controller.doOnChangeStart(block: (controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerListener =
    addListener(onChangeStart = block)

fun Controller.doOnChangeEnd(block: (controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerListener =
    addListener(onChangeEnd = block)

fun Controller.addListener(
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
    onChangeStart: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerListener = ControllerListener(
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
).also { addListener(it) }

fun Router.doOnControllerPreCreate(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle?) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, preCreate = block)

fun Router.doOnControllerPostCreate(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle?) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, postCreate = block)

fun Router.doOnControllerPreBuildView(
    recursive: Boolean = false,
    block: (controller: Controller, savedViewState: Bundle?) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, preBuildView = block)

fun Router.doOnControllerPostBuildView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, postBuildView = block)

fun Router.doOnControllerPreBindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, preBindView = block)

fun Router.doOnControllerPostBindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, postBindView = block)

fun Router.doOnControllerPreAttach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, preAttach = block)

fun Router.doOnControllerPostAttach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, postAttach = block)

fun Router.doOnControllerPreDetach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, preDetach = block)

fun Router.doOnControllerPostDetach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, postDetach = block)

fun Router.doOnControllerPreUnbindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, preUnbindView = block)

fun Router.doOnControllerPostUnbindView(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, postUnbindView = block)

fun Router.doOnControllerPreDestroy(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, preDestroy = block)

fun Router.doOnControllerPostDestroy(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, postDestroy = block)

fun Router.doOnControllerRestoreInstanceState(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, onRestoreInstanceState = block)

fun Router.doOnControllerSaveInstanceState(
    recursive: Boolean = false,
    block: (controller: Controller, outState: Bundle) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, onSaveInstanceState = block)

fun Router.doOnControllerRestoreViewState(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, onRestoreViewState = block)

fun Router.doOnControllerSaveViewState(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, outState: Bundle) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, onSaveViewState = block)

fun Router.doOnControllerChangeStart(
    recursive: Boolean = false,
    block: (controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, onChangeStart = block)

fun Router.doOnControllerChangeEnd(
    recursive: Boolean = false,
    block: (controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerListener =
    addControllerListener(recursive = recursive, onChangeEnd = block)

fun Router.addControllerListener(
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
    onChangeStart: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    onChangeEnd: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
): ControllerListener {
    return ControllerListener(
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
    ).also { addControllerListener(it, recursive) }
}

private class LambdaControllerListener(
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
    private val onChangeStart: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    private val onChangeEnd: ((controller: Controller, other: Controller?, changeHandler: ChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
) : ControllerListener {
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
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeStart?.invoke(controller, other, changeHandler, changeType)
    }

    override fun onChangeEnd(
        controller: Controller,
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeEnd?.invoke(controller, other, changeHandler, changeType)
    }
}