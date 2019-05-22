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
import com.ivianuu.director.internal.TransactionIndexer

/**
 * Hosts a group of [Router]s
 */
class RouterManager(
    val host: Any,
    private var postponeFullRestore: Boolean = false
) {

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

    private var routerStates: Map<Router, Bundle>? = null

    internal val transactionIndexer = TransactionIndexer()

    /**
     * Will be used to instantiate controllers after config changes or process death
     */
    var controllerFactory: ControllerFactory =
        DirectorPlugins.defaultControllerFactory ?: ReflectiveControllerFactory()

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
     * Restores the instance state of all routers
     */
    fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            restoreBasicState(it)

            transactionIndexer.restoreInstanceState(
                it.getBundle(KEY_TRANSACTION_INDEXER)!!
            )

            if (!postponeFullRestore) {
                restoreFullState()
            }
        }
    }

    /**
     * Saves the instance state of all containing routers
     */
    fun saveInstanceState(): Bundle = Bundle().apply {
        putBundle(
            KEY_TRANSACTION_INDEXER,
            transactionIndexer.saveInstanceState()
        )
        val routerStates = _routers.map { it.saveInstanceState() }
        putParcelableArrayList(KEY_ROUTER_STATES, ArrayList(routerStates))
    }

    /**
     * Let routers handle the back press
     */
    fun handleBack(): Boolean {
        return _routers
            .flatMap { it.backstack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .filter { it.isAttached }
            .map { it.router }
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
                Router.fromBundle(routerState, this) to routerState
            }
            .onEach { _routers.add(it.first) }
            .toMap()
    }

    private fun restoreFullState() {
        routerStates
            ?.filterKeys { _routers.contains(it) }
            ?.forEach { it.key.restoreInstanceState(it.value) }
        routerStates = null
    }

    private fun Router.restoreContainer() {
        if (!hasContainer) {
            rootView?.findViewById<ViewGroup>(containerId)?.let { setContainer(it) }
        }
    }

    private companion object {
        private const val KEY_ROUTER_STATES = "RouterManager.routerState"
        private const val KEY_TRANSACTION_INDEXER = "RouterManager.transactionIndexer"
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

fun RouterManager.getControllerByTagOrNull(tag: String): Controller? {
    for (router in routers) {
        val controller = router.getControllerByTagOrNull(tag)
        if (controller != null) return controller
    }

    return null
}

fun RouterManager.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun RouterManager.getControllerByInstanceIdOrNull(instanceId: String): Controller? {
    for (router in routers) {
        val controller = router.getControllerByInstanceIdOrNull(instanceId)
        if (controller != null) return controller
    }

    return null
}

fun RouterManager.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")