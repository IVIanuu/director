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

package com.ivianuu.director.androidx.lifecycle

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.ivianuu.director.Controller
import com.ivianuu.director.activity
import com.ivianuu.director.doOnPostDestroy
import com.ivianuu.director.retained.retained

/**
 * A [ViewModelStoreOwner] for [Controller]s
 */
class ControllerViewModelStoreOwner(controller: Controller) : ViewModelStoreOwner {

    private val _viewModelStore by controller.retained(KEY_VIEW_MODEL_STORE) { ViewModelStore() }

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

private val viewModelStoreOwnersByController = mutableMapOf<Controller, ViewModelStoreOwner>()

/**
 * The cached view model store owner of this controller
 */
val Controller.viewModelStoreOwner: ViewModelStoreOwner
    get() {
        return viewModelStoreOwnersByController.getOrPut(this) {
            ControllerViewModelStoreOwner(this)
                .also {
                    doOnPostDestroy { viewModelStoreOwnersByController.remove(this) }
                }
        }
    }

/**
 * The view model store of this controller
 */
val Controller.viewModelStore: ViewModelStore get() = viewModelStoreOwner.viewModelStore

/**
 * Returns a view model provider which uses the [viewModelStore] and the [factory]
 */
fun Controller.viewModelProvider(
    factory: ViewModelProvider.Factory = ViewModelProvider.NewInstanceFactory()
): ViewModelProvider =
    ViewModelProvider(viewModelStore, factory)