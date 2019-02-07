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

package com.ivianuu.director.permission

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Controller
import com.ivianuu.director.activity

/**
 * Permission changeHandler
 */
class PermissionHandler : Fragment() {

    private val callbacks =
        mutableMapOf<Int, MutableSet<PermissionCallback>>()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        callbacks[requestCode]?.let { callbacks ->
            callbacks.forEach {
                it.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean =
        callbacks.entries.flatMap { it.value }.any {
            it.shouldShowRequestPermissionRationale(requireActivity(), permission)
        }

    internal fun registerCallback(
        requestCode: Int,
        callback: PermissionCallback
    ) {
        callbacks.getOrPut(requestCode) { mutableSetOf() }
            .add(callback)
    }

    internal fun unregisterCallback(
        requestCode: Int,
        callback: PermissionCallback
    ) {
        val callbacksForCode = callbacks[requestCode] ?: return
        callbacksForCode.remove(callback)
        if (callbacksForCode.isEmpty()) {
            callbacks.remove(requestCode)
        }
    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.permission.PermissionHandler"

        internal fun get(controller: Controller): PermissionHandler {
            val activity = (controller.activity as? FragmentActivity)
                ?: error("controller is not attached to a FragmentActivity")
            return ((activity as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(FRAGMENT_TAG) as? PermissionHandler
                ?: PermissionHandler().also {
                    activity.supportFragmentManager.beginTransaction()
                        .add(it, FRAGMENT_TAG)
                        .commitNow()
                })
        }

    }

}