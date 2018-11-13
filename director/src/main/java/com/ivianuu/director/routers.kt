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
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.internal.LifecycleHandler
import com.ivianuu.director.internal.requireMainThread

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
@JvmName("attachRouterWithReceiver")
fun FragmentActivity.attachRouter(
    container: ViewGroup,
    savedInstanceState: Bundle?,
    controllerFactory: ControllerFactory? = null
) = attachRouter(this, container, savedInstanceState, controllerFactory)

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
fun attachRouter(
    activity: FragmentActivity,
    container: ViewGroup,
    savedInstanceState: Bundle? = null,
    controllerFactory: ControllerFactory? = null
): Router {
    requireMainThread()

    val lifecycleHandler = LifecycleHandler.install(activity)

    val router = lifecycleHandler.getRouter(container, savedInstanceState, controllerFactory)
    router.rebindIfNeeded()

    return router
}