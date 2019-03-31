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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerState
import com.ivianuu.director.addListener
import com.ivianuu.director.doOnPostDestroy

/**
 * A [LifecycleOwner] for [Controller]s
 */
class ControllerLifecycleOwner(controller: Controller) : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        with(lifecycleRegistry) {
            controller.addListener(
                postCreate = { _, _ -> handleLifecycleEvent(ON_CREATE) },
                postAttach = { _, _ ->
                    handleLifecycleEvent(ON_START)
                    handleLifecycleEvent(ON_RESUME)
                },
                preDetach = { _, _ ->
                    handleLifecycleEvent(ON_PAUSE)
                    handleLifecycleEvent(ON_STOP)
                },
                preDestroy = { handleLifecycleEvent(ON_DESTROY) }
            )

            when (controller.state) {
                ControllerState.DESTROYED -> markState(Lifecycle.State.DESTROYED)
                ControllerState.INITIALIZED -> markState(Lifecycle.State.INITIALIZED)
                ControllerState.CREATED -> markState(Lifecycle.State.CREATED)
                ControllerState.VIEW_CREATED -> markState(Lifecycle.State.STARTED)
                ControllerState.ATTACHED -> markState(Lifecycle.State.RESUMED)
            }
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

}

private val lifecycleOwnersByController =
    mutableMapOf<Controller, LifecycleOwner>()

/**
 * The cached lifecycle owner of this controller
 */
val Controller.lifecycleOwner: LifecycleOwner
    get() {
        return lifecycleOwnersByController.getOrPut(this) {
            ControllerLifecycleOwner(this)
                .also {
                    doOnPostDestroy { lifecycleOwnersByController.remove(this) }
                }
        }
    }

/**
 * The lifecycle of this controller
 */
val Controller.lifecycle: Lifecycle get() = lifecycleOwner.lifecycle