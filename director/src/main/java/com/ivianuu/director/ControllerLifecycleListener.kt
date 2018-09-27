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

import android.content.Context
import android.os.Bundle
import android.view.View

/**
 * Allows external classes to listen for lifecycle events of a [Controller]
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

    fun preContextAvailable(controller: Controller) {
    }

    fun postContextAvailable(controller: Controller, context: Context) {
    }

    fun preContextUnavailable(controller: Controller, context: Context) {
    }

    fun postContextUnavailable(controller: Controller) {
    }

    fun onSaveInstanceState(controller: Controller, outState: Bundle) {
    }

    fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
    }

    fun onSaveViewState(controller: Controller, outState: Bundle) {
    }

    fun onRestoreViewState(controller: Controller, savedViewState: Bundle) {
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
