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

package com.ivianuu.director.util

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.Router
import com.ivianuu.director.RouterDelegate

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TestActivity : Activity() {

    var changingConfigurations = false
    var isDestroying = false

    private lateinit var delegate: RouterDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = RouterDelegate(this, savedInstanceState?.getBundle(KEY_ROUTER_STATES))
    }

    override fun onStart() {
        super.onStart()
        delegate.hostStarted()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_ROUTER_STATES, delegate.saveInstanceState())
    }

    override fun onStop() {
        super.onStop()
        delegate.hostStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        delegate.hostDestroyed()
    }

    fun attachRouter(
        container: ViewGroup,
        savedInstanceState: Bundle? = null,
        controllerFactory: ControllerFactory? = null
    ): Router = delegate.getRouter(container, savedInstanceState, controllerFactory)

    override fun isChangingConfigurations(): Boolean = changingConfigurations

    override fun isDestroyed(): Boolean = isDestroying || super.isDestroyed()

    private companion object {
        private const val KEY_ROUTER_STATES = "TestActivity.routerStates"
    }
}