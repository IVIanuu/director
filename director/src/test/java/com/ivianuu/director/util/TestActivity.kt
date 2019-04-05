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
import com.ivianuu.director.Router
import com.ivianuu.director.RouterManager
import com.ivianuu.director.getRouter

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TestActivity : Activity() {

    var changingConfigurations = false
    var isDestroying = false

    private lateinit var manager: RouterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = RouterManager(this)
        savedInstanceState?.getBundle(KEY_ROUTER_STATES)?.let(manager::restoreInstanceState)
    }

    override fun onStart() {
        super.onStart()
        manager.onStart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_ROUTER_STATES, manager.saveInstanceState())
    }

    override fun onStop() {
        super.onStop()
        manager.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.onDestroy()
    }

    fun getRouter(
        container: ViewGroup,
        tag: String? = null
    ): Router = manager.getRouter(container, tag)

    override fun isChangingConfigurations(): Boolean = changingConfigurations

    override fun isDestroyed(): Boolean = isDestroying || super.isDestroyed()

    private companion object {
        private const val KEY_ROUTER_STATES = "TestActivity.routerStates"
    }
}