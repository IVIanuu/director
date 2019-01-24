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

    private val routers =
        mutableMapOf<Int, Router>()

    private var hasPreparedForHostDetach = false

    fun onCreate(savedInstanceState: Bundle?) {
        this.savedInstanceState = savedInstanceState
    }

    fun onStart() {
        hasPreparedForHostDetach = false
        routers.values.forEach { it.onActivityStarted() }
    }

    fun onResume() {
        routers.values.forEach { it.onActivityResumed() }
    }

    fun onPause() {
        routers.values.forEach { it.onActivityPaused() }
    }

    fun onSaveInstanceState(outState: Bundle) {
        prepareForHostDetachIfNeeded()

        routers.values.forEach { router ->
            val bundle = Bundle().also { router.saveInstanceState(it) }
            outState.putBundle(KEY_ROUTER_STATE_PREFIX + router.containerId, bundle)
        }
    }

    fun onStop() {
        prepareForHostDetachIfNeeded()
        routers.values.forEach { it.onActivityStopped() }
    }

    fun onDestroy() {
        routers.values.forEach { it.onActivityDestroyed() }
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
        return routers.getOrPut(container.id) {
            val routerState = savedInstanceState?.getBundle(KEY_ROUTER_STATE_PREFIX + container.id)
            Router(activity, container, routerState, controllerFactory)
        }
    }

    private fun prepareForHostDetachIfNeeded() {
        if (!hasPreparedForHostDetach) {
            hasPreparedForHostDetach = true
            routers.values.forEach { it.prepareForHostDetach() }
        }
    }

    private companion object {
        private const val KEY_ROUTER_STATE_PREFIX = "RouterDelegate.routerState"
    }
}