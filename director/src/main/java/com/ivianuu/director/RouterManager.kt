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

import android.os.Bundle
import android.view.ViewGroup

/**
 * Router delegate
 */
class RouterManager(
    private val host: Any,
    private val hostRouter: Router? = null,
    private var savedInstanceState: Bundle? = null
) {

    /**
     * All routers contained by this delegate
     */
    val routers: List<Router> get() = _routers
    private val _routers = mutableListOf<Router>()

    private var hostStarted = false

    fun hostStarted() {
        hostStarted = true
        _routers.forEach { it.hostStarted() }
    }

    fun hostStopped() {
        hostStarted = false
        _routers.forEach { it.hostStopped() }
    }

    fun hostDestroyed() {
        _routers.forEach {
            it.isBeingDestroyed = true
            it.removeContainer()
            it.hostDestroyed()
        }
    }

    fun saveInstanceState(): Bundle = Bundle().apply {
        _routers.forEach { router ->
            val bundle = router.saveInstanceState()
            putBundle(KEY_ROUTER_STATE_PREFIX + router.containerId, bundle)
        }
    }

    fun handleBack(): Boolean = _routers.any { it.handleBack() }

    fun getRouter(
        containerId: Int,
        tag: String? = null,
        controllerFactory: ControllerFactory? = null
    ): Router {
        var router = _routers.firstOrNull { it.containerId == containerId && it.tag == tag }
        if (router == null) {
            router = router {
                containerId(containerId)
                tag(tag)
                host(this@RouterManager.host)
                hostRouter(this@RouterManager.hostRouter)
                controllerFactory(controllerFactory)
                savedInstanceState(
                    this@RouterManager.savedInstanceState
                        ?.getBundle(KEY_ROUTER_STATE_PREFIX + containerId)
                )
            }

            _routers.add(router)

            if (hostStarted) {
                router.hostStarted()
            }

        }

        return router
    }

    fun getRouter(
        container: ViewGroup,
        tag: String? = null,
        controllerFactory: ControllerFactory? = null
    ): Router = getRouter(container.id, tag, controllerFactory).also {
        it.setContainer(container)
        it.rebind()
    }

    fun removeRouter(router: Router) {
        if (_routers.remove(router)) {
            router.setBackstack(emptyList())
            router.isBeingDestroyed = true
            router.hostStopped()
            router.removeContainer()
            router.hostDestroyed()
        }
    }

    private companion object {
        private const val KEY_ROUTER_STATE_PREFIX = "RouterManager.routerState"
    }
}