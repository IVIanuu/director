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
 * Hosts a group of routers
 */
class RouterManager(
    private val host: Any,
    private val hostRouter: Router? = null,
    savedInstanceState: Bundle? = null,
    private var postponeFullRestore: Boolean = false
) {

    /**
     * All routers contained by this delegate
     */
    val routers: List<Router> get() = _routers
    private val _routers = mutableListOf<Router>()

    private var hostStarted = false

    private var routerStates: Map<Router, Bundle>? = null

    init {
        restoreInstanceState(savedInstanceState)
    }

    fun hostStarted() {
        hostStarted = true
        _routers.forEach { it.hostStarted() }
    }

    fun hostStopped() {
        hostStarted = false
        _routers.forEach { it.hostStopped() }
    }

    fun hostIsBeingDestroyed() {
        _routers.forEach { it.isBeingDestroyed = true }
    }

    fun hostDestroyed() {
        _routers.forEach {
            it.isBeingDestroyed = true
            it.removeContainer()
            it.hostDestroyed()
        }
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            restoreBasicState(it)
            if (!postponeFullRestore) {
                restoreFullState()
            }
        }
    }

    fun saveInstanceState(): Bundle = Bundle().apply {
        val routerStates = _routers.map { it.saveInstanceState() }
        putParcelableArrayList(KEY_ROUTER_STATES, ArrayList(routerStates))
    }

    fun handleBack(): Boolean {
        return _routers
            .flatMap { it.backstack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .map { it.controller }
            .any { it.isAttached && it.router.handleBack() }
    }

    fun getRouterOrNull(containerId: Int, tag: String?): Router? =
        _routers.firstOrNull { it.containerId == containerId && it.tag == tag }

    fun getRouter(containerId: Int, tag: String?): Router {
        var router = getRouterOrNull(containerId, tag)
        if (router == null) {
            router = router {
                containerId(containerId)
                tag(tag)
                host(this@RouterManager.host)
                hostRouter(this@RouterManager.hostRouter)
            }

            _routers.add(router)
            if (hostStarted) {
                router.hostStarted()
            }
        }

        return router
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

    fun postponeRestore() {
        postponeFullRestore = true
    }

    fun startPostponedFullRestore() {
        if (postponeFullRestore) {
            postponeFullRestore = false
            restoreFullState()
        }
    }

    private fun restoreBasicState(savedInstanceState: Bundle) {
        val routerStates = savedInstanceState
            .getParcelableArrayList<Bundle>(KEY_ROUTER_STATES)!!

        _routers.clear()

        this.routerStates = routerStates
            .map { routerState ->
                // todo little hacky make this easier
                val containerId = routerState.getInt("Router.containerId")
                val tag = routerState.getString("Router.tag")

                router {
                    containerId(containerId)
                    tag(tag)
                    host(this@RouterManager.host)
                    hostRouter(this@RouterManager.hostRouter)
                } to routerState
            }
            .onEach { _routers.add(it.first) }
            .toMap()
    }

    private fun restoreFullState() {
        routerStates
            ?.filterKeys { _routers.contains(it) }
            ?.forEach {
                it.key.restoreInstanceState(it.value)
                it.key.rebind()
            }
        routerStates = null
    }

    private companion object {
        private const val KEY_ROUTER_STATES = "RouterManager.routerState"
    }
}

fun RouterManager.getRouterOrNull(container: ViewGroup, tag: String? = null): Router? =
    getRouterOrNull(container.id, tag)?.also {
        it.setContainer(container)
        it.rebind()
    }

fun RouterManager.getRouter(container: ViewGroup, tag: String? = null): Router =
    getRouter(container.id, tag).also {
        it.setContainer(container)
        it.rebind()
    }