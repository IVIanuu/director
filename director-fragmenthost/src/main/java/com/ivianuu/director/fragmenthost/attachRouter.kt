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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.Router

/**
 * Returns a [Router] for the [container]
 */
fun FragmentActivity.getRouter(
    container: ViewGroup,
    controllerFactory: ControllerFactory? = null
): Router = getRouter(supportFragmentManager, container, controllerFactory)

/**
 * Returns a [Router] for the [containerId]
 */
fun FragmentActivity.getRouter(
    containerId: Int,
    controllerFactory: ControllerFactory? = null
): Router = getRouter(
    findViewById<ViewGroup>(containerId),
    controllerFactory
)

/**
 * Returns a [Router] for the [container]
 */
fun Fragment.getRouter(
    container: ViewGroup,
    controllerFactory: ControllerFactory? = null
): Router = getRouter(childFragmentManager, container, controllerFactory)

/**
 * Returns a [Router] for the [containerId]
 */
fun Fragment.getRouter(
    containerId: Int,
    controllerFactory: ControllerFactory? = null
): Router = getRouter(
    view!!.findViewById<ViewGroup>(containerId),
    controllerFactory
)

private fun getRouter(
    fm: FragmentManager,
    container: ViewGroup,
    controllerFactory: ControllerFactory?
): Router {
    val routerHost = RouterHostFragment.install(fm)
    return routerHost.getRouter(container, controllerFactory)
}