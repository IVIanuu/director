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

package com.ivianuu.director.fragmenthost

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.Router

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
fun FragmentActivity.attachRouter(
    container: ViewGroup,
    controllerFactory: ControllerFactory? = null
): Router {
    val routerHost = RouterHostFragment.install(this)

    val router = routerHost.getRouter(container, controllerFactory)
    router.rebind()

    return router
}

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
fun FragmentActivity.attachRouter(
    containerId: Int,
    controllerFactory: ControllerFactory? = null
): Router = attachRouter(
    findViewById<ViewGroup>(containerId),
    controllerFactory
)