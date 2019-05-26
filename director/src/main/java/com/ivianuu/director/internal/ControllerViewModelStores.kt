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

package com.ivianuu.director.internal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.ivianuu.director.Controller

internal class ControllerViewModelStores : ViewModel() {

    private val viewModelStores = mutableMapOf<String, ViewModelStore>()

    fun getViewModelStore(instanceId: String): ViewModelStore =
        viewModelStores.getOrPut(instanceId) { ViewModelStore() }

    fun removeViewModelStore(instanceId: String) {
        viewModelStores.remove(instanceId)?.clear()
    }

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ControllerViewModelStores() as T
    }

    companion object {
        fun get(controller: Controller): ControllerViewModelStores {
            val provider = ViewModelProvider(controller.activity, Factory)
            return provider[ControllerViewModelStores::class.java]
        }
    }
}