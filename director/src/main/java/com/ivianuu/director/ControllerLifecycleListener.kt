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

class ControllerLifecycleListenerBuilder internal constructor() {

    private var preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null
    private var postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null
    private var preInflateView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null
    private var postInflateView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? =
        null
    private var preBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? =
        null
    private var postBindView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? =
        null
    private var preAttach: ((controller: Controller, view: View) -> Unit)? = null
    private var postAttach: ((controller: Controller, view: View) -> Unit)? = null
    private var preDetach: ((controller: Controller, view: View) -> Unit)? = null
    private var postDetach: ((controller: Controller, view: View) -> Unit)? = null
    private var preUnbindView: ((controller: Controller, view: View) -> Unit)? = null
    private var postUnbindView: ((controller: Controller) -> Unit)? = null
    private var preDestroy: ((controller: Controller) -> Unit)? = null
    private var postDestroy: ((controller: Controller) -> Unit)? = null
    private var onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null
    private var onSaveViewState: ((controller: Controller, outState: Bundle) -> Unit)? = null
    private var onChangeStart: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? =
        null
    private var onChangeEnd: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? =
        null

    fun preCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.preCreate = block
        }

    fun postCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.postCreate = block
        }

    fun preInflateView(block: (controller: Controller, savedViewState: Bundle?) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.preInflateView = block
        }

    fun postInflateView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.postInflateView = block
        }

    fun preBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.preBindView = block
        }

    fun postBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.postBindView = block
        }

    fun preAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.preAttach = block
        }

    fun postAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.postAttach = block
        }

    fun preDetach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.preDetach = block
        }

    fun postDetach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.postDetach = block
        }

    fun preUnbindView(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.preUnbindView = block
        }

    fun postUnbindView(block: (controller: Controller) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.postUnbindView = block
        }

    fun preDestroy(block: (controller: Controller) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.preDestroy = block
        }

    fun postDestroy(block: (controller: Controller) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.postDestroy = block
        }

    fun onSaveInstanceState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.onSaveInstanceState = block
        }

    fun onSaveViewState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.onSaveViewState = block
        }

    fun onChangeStart(block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.onChangeStart = block
        }

    fun onChangeEnd(block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListenerBuilder =
        apply {
            this.onChangeEnd = block
        }

    fun build(): ControllerLifecycleListener = LambdaLifecycleListener(
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
    )
}

/**
 * Returns a new [ControllerLifecycleListener] build by [init]
 */
fun ControllerLifecycleListener(init: ControllerLifecycleListenerBuilder.() -> Unit): ControllerLifecycleListener =
    ControllerLifecycleListenerBuilder().apply(init).build()

fun Controller.doOnPreCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { preCreate(block) }

fun Controller.doOnPostCreate(block: (controller: Controller, savedInstanceState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { postCreate(block) }

fun Controller.doOnPreInflateView(block: (controller: Controller, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { preInflateView(block) }

fun Controller.doOnPostInflateView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { postInflateView(block) }

fun Controller.doOnPreBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { preBindView(block) }

fun Controller.doOnPostBindView(block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { postBindView(block) }

fun Controller.doOnPreAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { preAttach(block) }

fun Controller.doOnPostAttach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { postAttach(block) }

fun Controller.doOnPreDetach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { preDetach(block) }

fun Controller.doOnPostDetach(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { postDetach(block) }

fun Controller.doOnPreUnbindView(block: (controller: Controller, view: View) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { preUnbindView(block) }

fun Controller.doOnPostUnbindView(block: (controller: Controller) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { postUnbindView(block) }

fun Controller.doOnPreDestroy(block: (controller: Controller) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { preDestroy(block) }

fun Controller.doOnPostDestroy(block: (controller: Controller) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { postDestroy(block) }

fun Controller.doOnSaveInstanceState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { onSaveInstanceState(block) }

fun Controller.doOnSaveViewState(block: (controller: Controller, outState: Bundle) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { onSaveViewState(block) }

fun Controller.doOnChangeStart(block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { onChangeStart(block) }

fun Controller.doOnChangeEnd(block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit): ControllerLifecycleListener =
    addLifecycleListener { onChangeEnd(block) }

fun Controller.addLifecycleListener(
    init: ControllerLifecycleListenerBuilder.() -> Unit
): ControllerLifecycleListener = ControllerLifecycleListener(init).also { addLifecycleListener(it) }

fun Router.doOnControllerPreCreate(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { preCreate(block) }

fun Router.doOnControllerPostCreate(
    recursive: Boolean = false,
    block: (controller: Controller, savedInstanceState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { postCreate(block) }

fun Router.doOnControllerPreInflateView(
    recursive: Boolean = false,
    block: (controller: Controller, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { preInflateView(block) }

fun Router.doOnControllerPostInflateView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { postInflateView(block) }

fun Router.doOnControllerPreBindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { preBindView(block) }

fun Router.doOnControllerPostBindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View, savedViewState: Bundle?) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { postBindView(block) }

fun Router.doOnControllerPreAttach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { preAttach(block) }

fun Router.doOnControllerPostAttach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { postAttach(block) }

fun Router.doOnControllerPreDetach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { preDetach(block) }

fun Router.doOnControllerPostDetach(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { postAttach(block) }

fun Router.doOnControllerPreUnbindView(
    recursive: Boolean = false,
    block: (controller: Controller, view: View) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { preUnbindView(block) }

fun Router.doOnControllerPostUnbindView(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { postUnbindView(block) }

fun Router.doOnControllerPreDestroy(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { preDestroy(block) }

fun Router.doOnControllerPostDestroy(
    recursive: Boolean = false,
    block: (controller: Controller) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { postDestroy(block) }

fun Router.doOnControllerSaveInstanceState(
    recursive: Boolean = false,
    block: (controller: Controller, outState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { onSaveInstanceState(block) }

fun Router.doOnControllerSaveViewState(
    recursive: Boolean = false,
    block: (controller: Controller, outState: Bundle) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { onSaveViewState(block) }

fun Router.doOnControllerChangeStart(
    recursive: Boolean = false,
    block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { onChangeStart(block) }

fun Router.doOnControllerChangeEnd(
    recursive: Boolean = false,
    block: (controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit
): ControllerLifecycleListener =
    addLifecycleListener(recursive) { onChangeEnd(block) }

fun Router.addLifecycleListener(
    recursive: Boolean = false,
    init: ControllerLifecycleListenerBuilder.() -> Unit
): ControllerLifecycleListener {
    return ControllerLifecycleListener(init).also { addLifecycleListener(it, recursive) }
}

private class LambdaLifecycleListener(
    private val preCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    private val postCreate: ((controller: Controller, savedInstanceState: Bundle?) -> Unit)? = null,
    private val preInflateView: ((controller: Controller, savedViewState: Bundle?) -> Unit)? = null,
    private val postInflateView: ((controller: Controller, view: View, savedViewState: Bundle?) -> Unit)? = null,
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
    private val onSaveInstanceState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    private val onSaveViewState: ((controller: Controller, outState: Bundle) -> Unit)? = null,
    private val onChangeStart: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null,
    private val onChangeEnd: ((controller: Controller, changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) -> Unit)? = null
) : ControllerLifecycleListener {
    override fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
        preCreate?.invoke(controller, savedInstanceState)
    }

    override fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
        postCreate?.invoke(controller, savedInstanceState)
    }

    override fun preInflateView(controller: Controller, savedViewState: Bundle?) {
        preInflateView?.invoke(controller, savedViewState)
    }

    override fun postInflateView(controller: Controller, view: View, savedViewState: Bundle?) {
        postInflateView?.invoke(controller, view, savedViewState)
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

    override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        onSaveInstanceState?.invoke(controller, outState)
    }

    override fun onSaveViewState(controller: Controller, outState: Bundle) {
        onSaveViewState?.invoke(controller, outState)
    }

    override fun onChangeStart(
        controller: Controller,
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeStart?.invoke(controller, changeHandler, changeType)
    }

    override fun onChangeEnd(
        controller: Controller,
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeEnd?.invoke(controller, changeHandler, changeType)
    }
}