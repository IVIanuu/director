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
import com.ivianuu.director.RouterManager
import com.ivianuu.director.getRouter
import com.ivianuu.director.getRouterOrNull

fun FragmentActivity.getRouterOrNull(containerId: Int, tag: String? = null): Router? =
    routerManager.getRouterOrNull(containerId, tag)

fun FragmentActivity.getRouterOrNull(container: ViewGroup, tag: String? = null): Router? =
    routerManager.getRouterOrNull(container, tag)

fun FragmentActivity.getRouter(
    containerId: Int,
    tag: String? = null,
    factory: () -> Router
): Router =
    routerManager.getRouter(containerId, tag, factory)

fun FragmentActivity.getRouter(
    container: ViewGroup,
    tag: String? = null,
    factory: () -> Router
): Router = routerManager.getRouter(container, tag, factory)

fun FragmentActivity.removeRouter(router: Router) {
    routerManager.removeRouter(router)
}

fun FragmentActivity.router(
    containerId: Int,
    tag: String? = null,
    factory: () -> Router
): Lazy<Router> =
    lazy(LazyThreadSafetyMode.NONE) { getRouter(containerId, tag, factory) }

fun FragmentActivity.postponeFullRestore() {
    RouterHostFragment.postponeFullRestore(this)
}

fun FragmentActivity.startPostponedFullRestore() {
    RouterHostFragment.startPostponedFullRestore(this)
}

val FragmentActivity.routerManager: RouterManager
    get() = RouterHostFragment.getManager(this)