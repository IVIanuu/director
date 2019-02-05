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

package com.ivianuu.director

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup

/**
 * Router delegate
 */
class RouterDelegate(
    private val activity: Activity,
    private var savedInstanceState: Bundle? = null
) {

    /**
     * All routers contained by this delegate
     */
    val routers: List<Router> get() = _routers.values.toList()
    private val _routers = mutableMapOf<Int, Router>()

    private var hostStarted = false

    fun hostStarted() {
        hostStarted = true
        _routers.values.forEach { it.hostStarted() }
    }

    fun hostStopped() {
        hostStarted = false
        _routers.values.forEach { it.hostStopped() }
    }

    fun hostDestroyed() {
        _routers.values.forEach { it.hostDestroyed() }
    }

    fun saveInstanceState(): Bundle = Bundle().apply {
        _routers.values.forEach { router ->
            val bundle = router.saveInstanceState()
            putBundle(KEY_ROUTER_STATE_PREFIX + router.containerId, bundle)
        }
    }

    fun getRouter(
        container: ViewGroup,
        controllerFactory: ControllerFactory?
    ): Router = getRouter(container, savedInstanceState, controllerFactory)

    fun getRouter(
        container: ViewGroup,
        savedInstanceState: Bundle?,
        controllerFactory: ControllerFactory?
    ): Router {
        return _routers.getOrPut(container.id) {
            val routerState = savedInstanceState?.getBundle(KEY_ROUTER_STATE_PREFIX + container.id)
            val router = Router(activity, container, routerState, controllerFactory)

            if (hostStarted) {
                router.hostStarted()
            }

            router
        }
    }

    fun getRouter(
        containerId: Int,
        controllerFactory: ControllerFactory?
    ): Router = getRouter(
        activity.findViewById<ViewGroup>(containerId),
        savedInstanceState,
        controllerFactory
    )

    fun getRouter(
        containerId: Int,
        savedInstanceState: Bundle?,
        controllerFactory: ControllerFactory?
    ): Router = getRouter(
        activity.findViewById<ViewGroup>(containerId),
        savedInstanceState,
        controllerFactory
    )

    private companion object {
        private const val KEY_ROUTER_STATE_PREFIX = "RouterDelegate.routerState"
    }
}