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

import android.view.ViewGroup

/**
 * Hosts a group of [Router]s
 */
class RouterManager(val parent: Controller? = null) {

    /**
     * All routers of this router manager
     */
    val routers: List<Router> get() = _routers
    private val _routers = mutableListOf<Router>()

    var isStarted = false
        private set
    var isDestroyed = false
        private set

    private var rootView: ViewGroup? = null

    /**
     * Sets the root view for all routers
     */
    fun setRootView(rootView: ViewGroup) {
        if (this.rootView != rootView) {
            this.rootView = rootView
            _routers.forEach { it.restoreContainer() }
        }
    }

    /**
     * Removes the previously added [rootView]
     */
    fun removeRootView() {
        if (rootView != null) {
            _routers.reversed().forEach { it.removeContainer() }
            rootView = null
        }
    }

    /**
     * Notifies that the host was started
     */
    fun onStart() {
        isStarted = true
        _routers.forEach { it.start() }
    }

    /**
     * Notifies that the host was stopped
     */
    fun onStop() {
        isStarted = false
        _routers.reversed().forEach { it.stop() }
    }

    /**
     * Notifies that the host was destroyed
     */
    fun onDestroy() {
        isDestroyed = true
        _routers.reversed().forEach {
            if (it.isStarted) {
                it.stop()
            }

            if (it.hasContainer) {
                it.removeContainer()
            }

            it.destroy()
        }
    }

    /**
     * Let routers handle the back press
     */
    fun handleBack(): Boolean {
        return _routers
            .flatMap { it.backstack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .filter { it.controller.isAttached }
            .map { it.controller.router }
            .any { it.handleBack() }
    }

    /**
     * Returns the router for [containerId] and [tag] or null
     */
    fun getRouterOrNull(containerId: Int, tag: String? = null): Router? =
        _routers.firstOrNull { it.containerId == containerId && it.tag == tag }

    /**
     * Returns the router for [containerId] and [tag] or creates a new one
     */
    fun getRouter(containerId: Int, tag: String? = null): Router {
        var router = getRouterOrNull(containerId, tag)
        if (router == null) {
            router = Router(containerId, tag, this)
            _routers.add(router)
            if (isDestroyed) {
                router.destroy()
            } else if (isStarted) {
                router.start()
            }
        }

        router.restoreContainer()

        return router
    }

    /**
     * Removes the previously added [router]
     */
    fun removeRouter(router: Router) {
        if (_routers.remove(router)) {
            if (router.isStarted) {
                router.stop()
            }
            if (router.hasContainer) {
                router.removeContainer()
            }
            router.destroy()
        }
    }

    private fun Router.restoreContainer() {
        if (!hasContainer) {
            rootView?.findViewById<ViewGroup>(containerId)?.let { setContainer(it) }
        }
    }

}

fun RouterManager.getRouterOrNull(container: ViewGroup, tag: String? = null): Router? {
    return getRouterOrNull(container.id, tag)?.also { it.setContainer(container) }
}

fun RouterManager.getRouter(container: ViewGroup, tag: String? = null): Router {
    return getRouter(container.id, tag).also { it.setContainer(container) }
}

fun RouterManager.router(containerId: Int, tag: String? = null): Lazy<Router> =
    lazy(LazyThreadSafetyMode.NONE) { getRouter(containerId, tag) }

fun RouterManager.findControllerByTag(tag: String): Controller? {
    for (router in routers) {
        val controller = router.findControllerByTag(tag)
        if (controller != null) return controller
    }

    return null
}