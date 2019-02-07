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

package com.ivianuu.director.fragmenthost

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Router
import com.ivianuu.director.RouterBuilder

fun FragmentActivity.getRouterOrNull(containerId: Int, tag: String? = null): Router? =
    routerHostFragment.getRouterOrNull(containerId, tag)

fun FragmentActivity.getRouterOrNull(container: ViewGroup, tag: String? = null): Router? =
    routerHostFragment.getRouterOrNull(container, tag)

fun FragmentActivity.getRouter(containerId: Int, tag: String? = null): Router =
    routerHostFragment.getRouter(containerId, tag)

fun FragmentActivity.getRouter(container: ViewGroup, tag: String? = null): Router =
    routerHostFragment.getRouter(container, tag)

fun FragmentActivity.getOrCreateRouter(
    containerId: Int,
    tag: String? = null,
    init: RouterBuilder.() -> Unit = {}
): Router = routerHostFragment.getOrCreateRouter(containerId, tag, init)

fun FragmentActivity.getOrCreateRouter(
    container: ViewGroup,
    tag: String? = null,
    init: RouterBuilder.() -> Unit = {}
): Router = routerHostFragment.getOrCreateRouter(container, tag, init)

fun FragmentActivity.removeRouter(router: Router) =
    routerHostFragment.removeRouter(router)

private val FragmentActivity.routerHostFragment: RouterHostFragment
    get() =
        RouterHostFragment.install(supportFragmentManager)