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

package com.ivianuu.director.activitycallbacks

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Controller
import com.ivianuu.director.activity

/**
 * Handles activity results of controllers
 */
class ActivityCallbacks : Fragment() {

    private val activityResultListeners =
        mutableMapOf<Int, MutableSet<ActivityResultListener>>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityByCallbacks.put(this, requireActivity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        activityResultListeners[requestCode]?.toSet()?.let { listeners ->
            listeners.forEach { it.onActivityResult(requestCode, resultCode, data) }
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
        mutableMapOf<Int, MutableSet<PermissionListener>>()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionResultListeners[requestCode]?.toSet()?.let { listeners ->
            listeners.forEach {
                it.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    internal fun addPermissionResultListener(
        requestCode: Int,
        listener: PermissionListener
    ) {
        permissionResultListeners.getOrPut(requestCode) { mutableSetOf() }
            .add(listener)
    }

    internal fun removePermissionResultListener(
        requestCode: Int,
        listener: PermissionListener
    ) {
        val callbacksForCode = permissionResultListeners[requestCode] ?: return
        callbacksForCode.remove(listener)
        if (callbacksForCode.isEmpty()) {
            permissionResultListeners.remove(requestCode)
        }
    }

    private val multiWindowModeChangeListeners =
        mutableSetOf<MultiWindowModeChangeListener>()

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        multiWindowModeChangeListeners.toList().forEach {
            it.onMultiWindowModeChanged(isInMultiWindowMode)
        }
    }

    internal val isInMultiWindowMode: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activityByCallbacks[this]!!.isInMultiWindowMode
        } else {
            false
        }

    internal fun addMultiWindowModeChangeListener(listener: MultiWindowModeChangeListener) {
        multiWindowModeChangeListeners.add(listener)
    }

    internal fun removeMultiWindowModeChangeListener(listener: MultiWindowModeChangeListener) {
        multiWindowModeChangeListeners.remove(listener)
    }

    private val pictureInPictureModeChangeListeners =
        mutableSetOf<PictureInPictureModeChangeListener>()

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        pictureInPictureModeChangeListeners.toSet().forEach {
            it.onPictureInPictureModeChanged(isInPictureInPictureMode)
        }
    }

    internal val isInPictureInPictureMode: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activityByCallbacks[this]!!.isInPictureInPictureMode
        } else {
            false
        }

    internal fun addPictureInPictureModeChangeListener(listener: PictureInPictureModeChangeListener) {
        pictureInPictureModeChangeListeners.add(listener)
    }

    internal fun removePictureInPictureModeChangeListener(listener: PictureInPictureModeChangeListener) {
        pictureInPictureModeChangeListeners.remove(listener)
    }

    override fun onDetach() {
        super.onDetach()
        activityByCallbacks.remove(this)
    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.activitycallbacks.ActivityCallbacks"

        private val activityByCallbacks = mutableMapOf<ActivityCallbacks, FragmentActivity>()

        internal fun get(controller: Controller): ActivityCallbacks {
            val activity = (controller.activity as? FragmentActivity)
                ?: error("controller is not attached to a FragmentActivity")
            return ((activity as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(FRAGMENT_TAG) as? ActivityCallbacks
                ?: ActivityCallbacks().also {
                    activity.supportFragmentManager.beginTransaction()
                        .add(
                            it,
                            FRAGMENT_TAG
                        )
                        .commitNow()
                }).also {
                activityByCallbacks[it] = activity
            }
        }

    }

}

internal val Controller.activityCallbacks: ActivityCallbacks
    get() = ActivityCallbacks.get(this)