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

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Handles activity results of controllers
 */
class ActivityCallbacks : Fragment() {

    private val activityResultListeners =
        mutableMapOf<Int, MutableSet<ActivityResultListener>>()

    init {
        retainInstance = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activities[this] = requireActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        activityResultListeners[requestCode]?.toSet()?.let { listeners ->
            listeners.forEach { it(requestCode, resultCode, data) }
        }
    }

    internal fun addActivityResultListener(
        requestCode: Int,
        listener: ActivityResultListener
    ) {
        activityResultListeners.getOrPut(requestCode) { mutableSetOf() }
            .add(listener)
    }

    internal fun removeActivityResultListener(
        requestCode: Int,
        listener: ActivityResultListener
    ) {
        val listenersForCode = activityResultListeners[requestCode] ?: return
        listenersForCode.remove(listener)
        if (listenersForCode.isEmpty()) {
            activityResultListeners.remove(requestCode)
        }
    }

    private val permissionResultListeners =
        mutableMapOf<Int, MutableSet<PermissionResultListener>>()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionResultListeners[requestCode]?.toSet()?.let { listeners ->
            listeners.forEach {
                it(requestCode, permissions, grantResults)
            }
        }
    }

    internal fun addPermissionResultListener(
        requestCode: Int,
        listener: PermissionResultListener
    ) {
        permissionResultListeners.getOrPut(requestCode) { mutableSetOf() }
            .add(listener)
    }

    internal fun removePermissionResultListener(
        requestCode: Int,
        listener: PermissionResultListener
    ) {
        val callbacksForCode = permissionResultListeners[requestCode] ?: return
        callbacksForCode.remove(listener)
        if (callbacksForCode.isEmpty()) {
            permissionResultListeners.remove(requestCode)
        }
    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.ActivityCallbacks"

        private val activities =
            mutableMapOf<ActivityCallbacks, FragmentActivity>()

        internal fun get(controller: Controller): ActivityCallbacks {
            return controller.requireActivity().supportFragmentManager
                .findFragmentByTag(FRAGMENT_TAG) as? ActivityCallbacks
                ?: ActivityCallbacks().also {
                    controller.requireActivity().supportFragmentManager.beginTransaction()
                        .add(it, FRAGMENT_TAG)
                        .commitNow()
                }.also {
                    if (it.activity == null) {
                        activities[it] = controller.requireActivity()
                    }
                }
        }

    }

}

internal val Controller.activityCallbacks: ActivityCallbacks
    get() = ActivityCallbacks.get(this)

/**
 * Listener for activity results
 */
typealias ActivityResultListener = (requestCode: Int, resultCode: Int, data: Intent?) -> Unit

/**
 * Notifies the [listener] on activity results for [requestCode]
 */
fun Controller.addActivityResultListener(
    requestCode: Int,
    listener: ActivityResultListener
) {
    activityCallbacks.addActivityResultListener(requestCode, listener)
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (lifecycle.currentState == DESTROYED) {
                activityCallbacks
                    .removeActivityResultListener(requestCode, listener)
            }
        }
    })
}

/**
 * Removes the previously added [listener]
 */
fun Controller.removeActivityResultListener(
    requestCode: Int,
    listener: ActivityResultListener
) {
    activityCallbacks.removeActivityResultListener(requestCode, listener)
}

/**
 * Starts the intent for result
 */
fun Controller.startActivityForResult(
    intent: Intent,
    requestCode: Int,
    options: Bundle? = null
) {
    activityCallbacks.startActivityForResult(intent, requestCode, options)
}

/**
 * Lister for permission results
 */
typealias PermissionResultListener = (requestCode: Int, permissions: Array<out String>, grantResults: IntArray) -> Unit

/**
 * Notifies the [listener] on activity results for [requestCode]
 */
fun Controller.addPermissionResultListener(
    requestCode: Int,
    listener: PermissionResultListener
) {
    activityCallbacks
        .addPermissionResultListener(requestCode, listener)

    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (lifecycle.currentState == DESTROYED) {
                activityCallbacks
                    .removePermissionResultListener(requestCode, listener)
            }
        }
    })
}

/**
 * Removes the previously added [listener]
 */
fun Controller.removePermissionResultListener(
    requestCode: Int,
    listener: PermissionResultListener
) {
    activityCallbacks
        .removePermissionResultListener(requestCode, listener)
}

@TargetApi(23)
fun Controller.requestPermissions(permissions: Array<out String>, requestCode: Int) {
    activityCallbacks
        .requestPermissions(permissions, requestCode)
}