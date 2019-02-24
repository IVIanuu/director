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
import com.ivianuu.stdlibx.firstNotNullResultOrNull

/**
 * Hosts a group of [Router]s
 */
class RouterManager(
    val host: Any,
    val hostRouterManager: RouterManager? = null,
    savedInstanceState: Bundle? = null,
    private var postponeFullRestore: Boolean = false
) {

    val routers: List<Router> get() = _routers
    private val _routers = mutableListOf<Router>()

    private var hostStarted = false
    private var routerStates: Map<Router, Bundle>? = null

    private var rootView: ViewGroup? = null

    internal val transactionIndexer: TransactionIndexer by lazy(LazyThreadSafetyMode.NONE) {
        hostRouterManager?.transactionIndexer ?: TransactionIndexer()
    }

    init {
        restoreInstanceState(savedInstanceState)
    }

    /**
     * Sets the root view for all routers
     */
    fun setContainers(rootView: ViewGroup) {
        if (this.rootView != rootView) {
            this.rootView = rootView
            _routers.forEach { it.restoreContainer() }
        }
    }

    /**
     * Notifies that the host was started
     */
    fun hostStarted() {
        hostStarted = true
        _routers.forEach { it.hostStarted() }
    }

    /**
     * Notifies that the host was stopped
     */
    fun hostStopped() {
        hostStarted = false
        _routers.reversed().forEach { it.hostStopped() }
    }

    /**
     * Removes the previously added [rootView]
     */
    fun removeContainers() {
        if (rootView != null) {
            _routers.reversed().forEach { it.removeContainer() }
            rootView = null
        }
    }

    /**
     * Notifies that the host is going to be destroyed
     */
    fun hostIsBeingDestroyed() {
        _routers.reversed().forEach { it.isBeingDestroyed = true }
    }

    /**
     * Notifies that the host was destroyed
     */
    fun hostDestroyed() {
        _routers.reversed().forEach {
            it.isBeingDestroyed = true
            it.removeContainer()
            it.hostDestroyed()
        }
    }

    /**
     * Restores the instance state of all routers
     */
    fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            restoreBasicState(it)

            if (hostRouterManager == null) {
                transactionIndexer.restoreInstanceState(
                    it.getBundle(KEY_TRANSACTION_INDEXER)!!
                )
            }

            if (!postponeFullRestore) {
                restoreFullState()
            }
        }
    }

    /**
     * Saves the instance state of all containing routers
     */
    fun saveInstanceState(): Bundle = Bundle().apply {
        if (hostRouterManager == null) {
            putBundle(
                KEY_TRANSACTION_INDEXER,
                transactionIndexer.saveInstanceState()
            )
        }
        val routerStates = _routers.map { it.saveInstanceState() }
        putParcelableArrayList(KEY_ROUTER_STATES, ArrayList(routerStates))
    }

    /**
     * Let routers handle the back
     */
    fun handleBack(): Boolean {
        return _routers
            .flatMap { it.backstack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .map { it.controller }
            .any { it.isAttached && it.router.handleBack() }
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
            if (hostStarted) {
                router.hostStarted()
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
            router.setBackstack(emptyList(), false)
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
                Router.fromBundle(routerState, this) to routerState
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

    private fun Router.restoreContainer() {
        if (!hasContainer) {
            rootView?.findViewById<ViewGroup>(containerId)?.let {
                setContainer(it)
                rebind()
            }
        }
    }

    private companion object {
        private const val KEY_ROUTER_STATES = "RouterManager.routerState"
        private const val KEY_TRANSACTION_INDEXER = "Router.transactionIndexer"
    }
}

internal val RouterManager.rootRouterManager: RouterManager
    get() = hostRouterManager?.rootRouterManager ?: this

fun RouterManager.getRouterOrNull(container: ViewGroup, tag: String? = null): Router? =
    getRouterOrNull(container.id, tag)?.also {
        if (!it.hasContainer) {
            it.setContainer(container)
            it.rebind()
        }
    }

fun RouterManager.getRouter(container: ViewGroup, tag: String? = null): Router =
    getRouter(container.id, tag).also {
        if (!it.hasContainer) {
            it.setContainer(container)
            it.rebind()
        }
    }

fun RouterManager.router(containerId: Int, tag: String? = null): Lazy<Router> =
    lazy(LazyThreadSafetyMode.NONE) { getRouter(containerId, tag) }

fun RouterManager.getControllerByTagOrNull(tag: String): Controller? =
    routers.firstNotNullResultOrNull { it.getControllerByTagOrNull(tag) }

fun RouterManager.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun RouterManager.getControllerByInstanceIdOrNull(instanceId: String): Controller? =
    routers.firstNotNullResultOrNull { it.getControllerByInstanceIdOrNull(instanceId) }

fun RouterManager.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")