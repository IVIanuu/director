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

package com.ivianuu.director.sample.controller

import androidx.lifecycle.Lifecycle.State.STARTED
import com.ivianuu.director.Router
import com.ivianuu.director.childRouter
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

class MultipleChildRouterController : BaseController() {

    override val layoutRes get() = R.layout.controller_multiple_child_routers
    override val toolbarTitle: String?
        get() = "Child router Demo"

    private val childRouters = mutableListOf<Router>()

    override fun onCreate() {
        super.onCreate()
        listOf(R.id.container_0, R.id.container_1, R.id.container_2)
            .map { childRouter(it) }
            .forEach {
                childRouters += it

                it.setRoot(
                    NavigationController(
                        0,
                        NavigationController.DisplayUpMode.HIDE,
                        false
                    ).toTransaction()
                )
            }
    }

    override fun handleBack(): Boolean {
        return childRouters
            .flatMap { it.backStack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .filter { it.controller.lifecycle.currentState.isAtLeast(STARTED) }
            .map { it.controller.router }
            .any { it.handleBack() }
    }
}