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

import android.os.Bundle
import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener
import com.ivianuu.scopes.lifecycle.AbstractLifecycle

/**
 * A [com.ivianuu.scopes.lifecycle.Lifecycle] for [Controller]s
 */
class ControllerLifecycle(
    controller: Controller
) : AbstractLifecycle<ControllerEvent>() {

    init {
        controller.addLifecycleListener(object : ControllerLifecycleListener {
            override fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
                super.preCreate(controller, savedInstanceState)
                onEvent(ControllerEvent.CREATE)
            }

            override fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
                super.preBindView(controller, view, savedViewState)
                onEvent(ControllerEvent.BIND_VIEW)
            }

            override fun preAttach(controller: Controller, view: View) {
                super.preAttach(controller, view)
                onEvent(ControllerEvent.ATTACH)
            }

            override fun postDetach(controller: Controller, view: View) {
                super.postDetach(controller, view)
                onEvent(ControllerEvent.DETACH)
            }

            override fun postUnbindView(controller: Controller) {
                super.postUnbindView(controller)
                onEvent(ControllerEvent.UNBIND_VIEW)
            }

            override fun postDestroy(controller: Controller) {
                super.postDestroy(controller)
                onEvent(ControllerEvent.DESTROY)
            }
        })
    }
}