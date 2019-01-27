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

package com.ivianuu.director.arch.lifecycle

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.ivianuu.director.Controller
import com.ivianuu.director.doOnPostDestroy
import com.ivianuu.director.retained.retainedLazy

/**
 * A [ViewModelStoreOwner] for [Controller]s
 */
class ControllerViewModelStoreOwner(controller: Controller) : ViewModelStoreOwner {

    private val _viewModelStore by controller.retainedLazy(KEY_VIEW_MODEL_STORE) { ViewModelStore() }

    init {
        controller.doOnPostDestroy {
            if (!it.activity.isChangingConfigurations) {
                _viewModelStore.clear()
            }
        }
    }

    override fun getViewModelStore(): ViewModelStore = _viewModelStore

    private companion object {
        private const val KEY_VIEW_MODEL_STORE = "ControllerViewModelStoreOwner.viewModelStore"
    }
}

fun Controller.ControllerViewModelStoreOwner(): ControllerViewModelStoreOwner = ControllerViewModelStoreOwner(this)