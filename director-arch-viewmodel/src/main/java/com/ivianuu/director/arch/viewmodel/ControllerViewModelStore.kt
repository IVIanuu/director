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

package com.ivianuu.director.arch.viewmodel

import androidx.lifecycle.ViewModelStore
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener

/**
 * A [ViewModelStore] for [Controller]
 */
class ControllerViewModelStore(controller: Controller) : ViewModelStore() {

    init {
        controller.addLifecycleListener(object : ControllerLifecycleListener {
            override fun postDestroy(controller: Controller) {
                super.postDestroy(controller)
                clear()
            }
        })
    }

}

/**
 * Returns a new [ControllerViewModelStore] for [this]
 */
fun Controller.ControllerViewModelStore() = ControllerViewModelStore(this)