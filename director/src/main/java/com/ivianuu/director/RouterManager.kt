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
    private var postponeFullRestore: Boolean = false
) {

    /**
     * All routers of this router manager
     */
    val routers: List<BaseRouter> get() = _routers
    private val _routers = mutableListOf<BaseRouter>()

    internal var isStarted = false
        private set
    internal var isDestroyed = false
        private set

    private var rootView: ViewGroup? = null

    private var routerStates: Map<BaseRouter, Bundle>? = null

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
            _routers.reversed()
                .filterIsInstance<ViewGroupRouter>()
                .forEach(ViewGroupRouter::removeContainer)
            rootView = null
        }
    }

    /**
     * Notifies that the host was started
     */
    fun onStart() {
        isStarted = true
        _routers.forEach(BaseRouter::start)
    }

    /**
     * Notifies that the host was stopped
     */
    fun onStop() {
        isStarted = false
        _routers.reversed().forEach(BaseRouter::stop)
    }

    /**
     * Notifies that the host was destroyed
     */
    fun onDestroy() {
        if (!isDestroyed) {
            isDestroyed = true
            _routers.reversed().forEach(BaseRouter::destroy)
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
        val routerStates = _routers.map(BaseRouter::saveInstanceState)
        putParcelableArrayList(KEY_ROUTER_STATES, ArrayList(routerStates))
    }

    /**
     * Let routers handle the back press
     */
    fun handleBack(): Boolean {
        return _routers.any(BaseRouter::handleBack)
    }

    /**
     * Returns the router for [id] and [tag] or null
     */
    fun getRouterOrNull(id: Int, tag: String? = null): BaseRouter? =
        _routers.firstOrNull { it.id == id && it.tag == tag }

    /**
     * Returns the router for [id] and [tag] or creates a new one
     */
    fun getRouter(id: Int, tag: String? = null): BaseRouter {
        var router = getRouterOrNull(id, tag)
        if (router == null) {
            router = Router(id, tag, this)
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
    fun removeRouter(router: BaseRouter) {
        if (_routers.remove(router)) {
            router.stop()
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
                BaseRouter.fromBundle(routerState) to routerState
            }
            .onEach {
                _routers.add(it.first)
            }
            .toMap()
    }

    private fun restoreFullState() {
        routerStates
            ?.filterKeys(_routers::contains)
            ?.forEach { it.key.restoreInstanceState(it.value) }
        routerStates = null
    }

    private fun BaseRouter.restoreContainer() {
        if (this !is ViewGroupRouter) return

        if (!hasContainer) {
            rootView?.findViewById<ViewGroup>(id)?.let(this::setContainer)
        }
    }

    private companion object {
        private const val KEY_ROUTER_STATES = "RouterManager.routerState"
        private const val KEY_TRANSACTION_INDEXER = "RouterManager.transactionIndexer"
    }
}

fun RouterManager.router(id: Int, tag: String? = null): Lazy<BaseRouter> =
    lazy(LazyThreadSafetyMode.NONE) { getRouter(id, tag) }

fun RouterManager.getControllerByTagOrNull(tag: String): Controller? =
    routers.firstNotNullResultOrNull { it.getControllerByTagOrNull(tag) }

fun RouterManager.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun RouterManager.getControllerByInstanceIdOrNull(instanceId: String): Controller? =
    routers.firstNotNullResultOrNull { it.getControllerByInstanceIdOrNull(instanceId) }

fun RouterManager.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")