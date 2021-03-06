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
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

fun Controller.childRouter(container: ViewGroup): Router =
    childRouter { container }

fun Controller.childRouter(containerId: Int): Router =
    childRouter { view!!.findViewById(containerId) }

fun Controller.childRouter(containerProvider: (() -> ViewGroup)? = null): Router {
    val router = Router(this)

    if (lifecycle.currentState == DESTROYED) {
        router.destroy()
        return router
    }

    if (containerProvider != null) {
        viewLifecycleOwnerLiveData.observe(this, Observer {
            it?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        ON_CREATE -> router.setContainer(containerProvider())
                        ON_DESTROY -> router.removeContainer()
                    }
                }
            })
        })
    }

    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                ON_RESUME -> router.start()
                ON_PAUSE -> router.stop()
                ON_DESTROY -> router.destroy()
            }
        }
    })

    return router
}