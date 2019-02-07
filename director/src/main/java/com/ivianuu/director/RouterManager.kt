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
        this.savedInstanceState = savedInstanceState

        if (savedInstanceState != null) {
            _routers.forEach { router ->
                this@RouterManager.savedInstanceState
                    ?.getBundle(KEY_ROUTER_STATE_PREFIX + router.containerId)
                    ?.let { router.restoreInstanceState(it) }
            }
        }
    }

    fun saveInstanceState(): Bundle = Bundle().apply {
        _routers.forEach { router ->
            val bundle = router.saveInstanceState()
            putBundle(KEY_ROUTER_STATE_PREFIX + router.containerId, bundle)
        }
    }

    fun handleBack(): Boolean {
        return _routers
            .flatMap { it.backstack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .map { it.controller }
            .any { it.isAttached && it.router.handleBack() }
    }

    fun getRouterOrNull(containerId: Int, tag: String? = null): Router? =
        _routers.firstOrNull { it.containerId == containerId && it.tag == tag }

    fun getRouter(containerId: Int, tag: String? = null): Router =
        getRouterOrNull(containerId, tag)
            ?: error("Couldn't find router for container id: $containerId and tag: $tag")

    fun getOrCreateRouter(
        containerId: Int,
        tag: String? = null,
        init: RouterBuilder.() -> Unit = {}
    ): Router {
        var router = getRouterOrNull(containerId, tag)
        if (router == null) {
            router = RouterBuilder().run {
                // defaults
                containerId(containerId)
                tag(tag)
                host(this@RouterManager.host)
                hostRouter(this@RouterManager.hostRouter)
                savedInstanceState(
                    this@RouterManager.savedInstanceState
                        ?.getBundle(KEY_ROUTER_STATE_PREFIX + containerId)
                )

                // user
                apply(init)

                check(containerId == containerId) { "Cannot change container id while using router manager" }
                check(tag == tag) { "Cannot change tag while using router manager" }
                check(host == this@RouterManager.host) { "Cannot change host while using router manager" }
                check(hostRouter == this@RouterManager.hostRouter) { "Cannot change host router while using router manager" }

                val router = build()
                _routers.add(router)
                if (hostStarted) {
                    router.hostStarted()
                }

                router
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

    private companion object {
        private const val KEY_ROUTER_STATE_PREFIX = "RouterManager.routerState"
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

fun RouterManager.getOrCreateRouter(
    container: ViewGroup,
    tag: String? = null,
    init: RouterBuilder.() -> Unit = {}
): Router = getOrCreateRouter(container.id, tag, init)
    .also {
        it.setContainer(container)
        it.rebind()
    }