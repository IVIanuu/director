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

package com.ivianuu.director.activityresult

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Controller

/**
 * Handles activity results of controllers
 */
class ActivityResultHandler : Fragment() {

    private val listeners =
        mutableMapOf<Int, MutableSet<ActivityResultListener>>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        listeners[requestCode]?.let { listeners ->
            listeners.forEach { it.onActivityResult(requestCode, resultCode, data) }
        }
    }

    internal fun registerListener(
        requestCode: Int,
        listener: ActivityResultListener
    ) {
        listeners.getOrPut(requestCode) { mutableSetOf() }
            .add(listener)
    }

    internal fun unregisterListener(
        requestCode: Int,
        listener: ActivityResultListener
    ) {
        val listenersForCode = listeners[requestCode] ?: return
        listenersForCode.remove(listener)
        if (listenersForCode.isEmpty()) {
            listeners.remove(requestCode)
        }
    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.activityresult.ActivityResultHandler"

        internal fun get(controller: Controller): ActivityResultHandler {
            val activity = (controller.activity as? FragmentActivity)
                ?: error("controller is not attached to a FragmentActivity")
            return ((activity as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(FRAGMENT_TAG) as? ActivityResultHandler
                ?: ActivityResultHandler().also {
                    activity.supportFragmentManager.beginTransaction()
                        .add(
                            it,
                            FRAGMENT_TAG
                        )
                        .commitNow()
                })
        }

    }

}