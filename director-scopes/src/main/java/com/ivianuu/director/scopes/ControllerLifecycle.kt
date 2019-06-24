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

package com.ivianuu.director.scopes

import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerState
import com.ivianuu.director.addLifecycleListener
import com.ivianuu.director.common.ControllerEvent
import com.ivianuu.director.common.ControllerEvent.ATTACH
import com.ivianuu.director.common.ControllerEvent.CREATE
import com.ivianuu.director.common.ControllerEvent.CREATE_VIEW
import com.ivianuu.director.common.ControllerEvent.DESTROY
import com.ivianuu.director.common.ControllerEvent.DESTROY_VIEW
import com.ivianuu.director.common.ControllerEvent.DETACH
import com.ivianuu.lifecycle.AbstractLifecycle

/**
 * A [com.ivianuu.scopes.lifecycle.Lifecycle] for [Controller]s
 */
class ControllerLifecycle(
    controller: Controller
) : AbstractLifecycle<ControllerEvent>() {

    init {
        controller.addLifecycleListener(
            preCreate = { onEvent(CREATE) },
            preCreateView = { onEvent(CREATE_VIEW) },
            preAttach = { _, _ -> onEvent(ATTACH) },
            postDetach = { _, _ -> onEvent(DETACH) },
            postDestroyView = { onEvent(DESTROY_VIEW) },
            postDestroy = { onEvent(DESTROY) }
        )

        when (controller.state) {
            ControllerState.DESTROYED -> onEvent(DESTROY)
            ControllerState.CREATED -> onEvent(CREATE)
            ControllerState.VIEW_CREATED -> onEvent(CREATE_VIEW)
            ControllerState.ATTACHED -> onEvent(ATTACH)
        }
    }
}