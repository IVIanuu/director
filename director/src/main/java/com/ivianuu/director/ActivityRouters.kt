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
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

fun ComponentActivity.router(container: ViewGroup): Router =
    router { container }

fun ComponentActivity.router(containerId: Int): Router =
    router { findViewById(containerId) }

fun ComponentActivity.router(containerProvider: (() -> ViewGroup)? = null): Router {
    val holder = ViewModelProvider(this, RouterHolder.Factory)[RouterHolder::class.java]
    val router = holder.router

    if (lifecycle.currentState == DESTROYED) {
        router.destroy()
        return router
    }

    if (containerProvider != null) router.setContainer(containerProvider())

    fun canHandleBack(): Boolean =
        router.backstack.isNotEmpty() &&
                (router.backstack.size > 1 || router.popsLastView)

    val backPressedCallback = object : OnBackPressedCallback(canHandleBack()) {
        override fun handleOnBackPressed() {
            router.handleBack()
        }
    }

    onBackPressedDispatcher.addCallback(backPressedCallback)

    val changeListener = router.doOnChangeStarted { _, _, _, _, _, _ ->
        backPressedCallback.isEnabled = canHandleBack()
    }

    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                ON_START -> router.start()
                ON_STOP -> router.stop()
                ON_DESTROY -> {
                    router.removeChangeListener(changeListener)

                    if (containerProvider != null) {
                        router.removeContainer()
                    }

                    if (!isChangingConfigurations) {
                        router.destroy()
                    } else if (containerProvider != null) {
                        router.removeContainer()
                    }
                }
            }
        }
    })

    return router
}

private class RouterHolder : ViewModel() {
    val router = Router()

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = RouterHolder() as T
    }
}