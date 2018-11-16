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

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener

/**
 * A [LifecycleOwner] for [Controller]s
 */
class ControllerLifecycleOwner(controller: Controller) : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    private val lifecycleListener = object : ControllerLifecycleListener {

        override fun postCreate(controller: Controller) {
            super.postCreate(controller)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun postBindView(controller: Controller, view: View) {
            super.postBindView(controller, view)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        override fun postAttach(controller: Controller, view: View) {
            super.postAttach(controller, view)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun preDetach(controller: Controller, view: View) {
            super.preDetach(controller, view)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        override fun preUnbindView(controller: Controller, view: View) {
            super.preUnbindView(controller, view)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        override fun preDestroy(controller: Controller) {
            super.preDestroy(controller)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    init {
        controller.addLifecycleListener(lifecycleListener)
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

}

/**
 * Returns a new [ControllerLifecycleOwner] for [this]
 */
fun Controller.ControllerLifecycleOwner() = ControllerLifecycleOwner(this)