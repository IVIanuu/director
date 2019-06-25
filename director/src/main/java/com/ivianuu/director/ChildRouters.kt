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

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle

fun Controller.childRouter(container: ViewGroup): Router =
    childRouter { container }

fun Controller.childRouter(containerId: Int): Router =
    childRouter { view!!.findViewById(containerId) }

fun Controller.childRouter(containerProvider: (() -> ViewGroup)? = null): Router {
    val router = Router(this)

    if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
        router.destroy()
        return router
    }

    if (view != null) {
        containerProvider?.invoke()?.let { router.setContainer(it) }
    }

    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        router.start()
    }

    addLifecycleListener(
        postCreateView = { _, _ ->
            containerProvider?.invoke()?.let { router.setContainer(it) }
        },
        postAttach = { _, _ ->
            router.start()
        },
        preDetach = { _, _ ->
            router.stop()
        },
        preDestroyView = { _, _ ->
            if (containerProvider != null) {
                router.removeContainer()
            }
        },
        preDestroy = {
            router.destroy()
        }
    )

    return router
}