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

import android.view.View

/**
 * Listener for [Controller]s
 */
interface ControllerLifecycleListener {

    fun preCreate(controller: Controller) {
    }

    fun postCreate(controller: Controller) {
    }

    fun preCreateView(controller: Controller) {
    }

    fun postCreateView(controller: Controller, view: View) {
    }

    fun preAttach(controller: Controller, view: View) {
    }

    fun postAttach(controller: Controller, view: View) {
    }

    fun preDetach(controller: Controller, view: View) {
    }

    fun postDetach(controller: Controller, view: View) {
    }

    fun preDestroyView(controller: Controller, view: View) {
    }

    fun postDestroyView(controller: Controller) {
    }

    fun preDestroy(controller: Controller) {
    }

    fun postDestroy(controller: Controller) {
    }

}

/**
 * Returns a new [ControllerLifecycleListener]
 */
fun ControllerLifecycleListener(
    preCreate: ((controller: Controller) -> Unit)? = null,
    postCreate: ((controller: Controller) -> Unit)? = null,
    preCreateView: ((controller: Controller) -> Unit)? = null,
    postCreateView: ((controller: Controller, view: View) -> Unit)? = null,
    preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    preDestroyView: ((controller: Controller, view: View) -> Unit)? = null,
    postDestroyView: ((controller: Controller) -> Unit)? = null,
    preDestroy: ((controller: Controller) -> Unit)? = null,
    postDestroy: ((controller: Controller) -> Unit)? = null
): ControllerLifecycleListener = LambdaControllerLifecycleListener(
    preCreate = preCreate, postCreate = postCreate,
    preCreateView = preCreateView, postCreateView = postCreateView,
    preAttach = preAttach, postAttach = postAttach,
    preDetach = preDetach, postDetach = postDetach,
    preDestroyView = preDestroyView, postDestroyView = postDestroyView,
    preDestroy = preDestroy, postDestroy = postDestroy
)

fun Controller.doOnPreCreate(block: (controller: Controller) -> Unit) =
    addLifecycleListener(preCreate = block)

fun Controller.doOnPostCreate(block: (controller: Controller) -> Unit) =
    addLifecycleListener(postCreate = block)

fun Controller.doOnPreCreateView(block: (controller: Controller) -> Unit) =
    addLifecycleListener(preCreateView = block)

fun Controller.doOnPostCreateView(block: (controller: Controller, view: View) -> Unit) =
    addLifecycleListener(postCreateView = block)

fun Controller.doOnPreAttach(block: (controller: Controller, view: View) -> Unit) =
    addLifecycleListener(preAttach = block)

fun Controller.doOnPostAttach(block: (controller: Controller, view: View) -> Unit) =
    addLifecycleListener(postAttach = block)

fun Controller.doOnPreDetach(block: (controller: Controller, view: View) -> Unit) =
    addLifecycleListener(preDetach = block)

fun Controller.doOnPostDetach(block: (controller: Controller, view: View) -> Unit) =
    addLifecycleListener(postDetach = block)

fun Controller.doOnPreDestroyView(block: (controller: Controller, view: View) -> Unit) =
    addLifecycleListener(preDestroyView = block)

fun Controller.doOnPostDestroyView(block: (controller: Controller) -> Unit) =
    addLifecycleListener(postDestroyView = block)

fun Controller.doOnPreDestroy(block: (controller: Controller) -> Unit) =
    addLifecycleListener(preDestroy = block)

fun Controller.doOnPostDestroy(block: (controller: Controller) -> Unit) =
    addLifecycleListener(postDestroy = block)

fun Controller.addLifecycleListener(
    preCreate: ((controller: Controller) -> Unit)? = null,
    postCreate: ((controller: Controller) -> Unit)? = null,
    preCreateView: ((controller: Controller) -> Unit)? = null,
    postCreateView: ((controller: Controller, view: View) -> Unit)? = null,
    preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    preDestroyView: ((controller: Controller, view: View) -> Unit)? = null,
    postDestroyView: ((controller: Controller) -> Unit)? = null,
    preDestroy: ((controller: Controller) -> Unit)? = null,
    postDestroy: ((controller: Controller) -> Unit)? = null
) = addLifecycleListener(
    ControllerLifecycleListener(
        preCreate = preCreate, postCreate = postCreate,
        preCreateView = preCreateView, postCreateView = postCreateView,
        preAttach = preAttach, postAttach = postAttach,
        preDetach = preDetach, postDetach = postDetach,
        preDestroyView = preDestroyView, postDestroyView = postDestroyView,
        preDestroy = preDestroy, postDestroy = postDestroy
    )
)

fun Router.addControllerLifecycleListener(
    recursive: Boolean = false,
    preCreate: ((controller: Controller) -> Unit)? = null,
    postCreate: ((controller: Controller) -> Unit)? = null,
    preCreateView: ((controller: Controller) -> Unit)? = null,
    postCreateView: ((controller: Controller, view: View) -> Unit)? = null,
    preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    preDestroyView: ((controller: Controller, view: View) -> Unit)? = null,
    postDestroyView: ((controller: Controller) -> Unit)? = null,
    preDestroy: ((controller: Controller) -> Unit)? = null,
    postDestroy: ((controller: Controller) -> Unit)? = null
) {
    return addControllerLifecycleListener(
        ControllerLifecycleListener(
            preCreate = preCreate,
            postCreate = postCreate,
            preCreateView = preCreateView,
            postCreateView = postCreateView,
            preAttach = preAttach,
            postAttach = postAttach,
            preDetach = preDetach,
            postDetach = postDetach,
            preDestroyView = preDestroyView,
            postDestroyView = postDestroyView,
            preDestroy = preDestroy,
            postDestroy = postDestroy
        ),

        recursive = recursive
    )
}

private class LambdaControllerLifecycleListener(
    private val preCreate: ((controller: Controller) -> Unit)? = null,
    private val postCreate: ((controller: Controller) -> Unit)? = null,
    private val preCreateView: ((controller: Controller) -> Unit)? = null,
    private val postCreateView: ((controller: Controller, view: View) -> Unit)? = null,
    private val preAttach: ((controller: Controller, view: View) -> Unit)? = null,
    private val postAttach: ((controller: Controller, view: View) -> Unit)? = null,
    private val preDetach: ((controller: Controller, view: View) -> Unit)? = null,
    private val postDetach: ((controller: Controller, view: View) -> Unit)? = null,
    private val preDestroyView: ((controller: Controller, view: View) -> Unit)? = null,
    private val postDestroyView: ((controller: Controller) -> Unit)? = null,
    private val preDestroy: ((controller: Controller) -> Unit)? = null,
    private val postDestroy: ((controller: Controller) -> Unit)? = null
) : ControllerLifecycleListener {
    override fun preCreate(controller: Controller) {
        preCreate?.invoke(controller)
    }

    override fun postCreate(controller: Controller) {
        postCreate?.invoke(controller)
    }

    override fun preCreateView(controller: Controller) {
        preCreateView?.invoke(controller)
    }

    override fun postCreateView(controller: Controller, view: View) {
        postCreateView?.invoke(controller, view)
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

    override fun preDestroyView(controller: Controller, view: View) {
        preDestroyView?.invoke(controller, view)
    }

    override fun postDestroyView(controller: Controller) {
        postDestroyView?.invoke(controller)
    }

    override fun preDestroy(controller: Controller) {
        preDestroy?.invoke(controller)
    }

    override fun postDestroy(controller: Controller) {
        postDestroy?.invoke(controller)
    }

}