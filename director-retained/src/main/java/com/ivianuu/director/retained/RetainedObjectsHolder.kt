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

package com.ivianuu.director.retained

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener

/**
 * Holds retained objects of controller
 */
class RetainedObjectsHolder : Fragment(), ControllerLifecycleListener {

    private val retainedObjects =
        mutableMapOf<String, RetainedObjects>()

    init {
        retainInstance = true
    }

    override fun postDestroy(controller: Controller) {
        super.postDestroy(controller)
        if (!controller.activity.isChangingConfigurations) {
            retainedObjects.remove(controller.instanceId)
        }
    }

    internal fun getRetainedObjects(controller: Controller): RetainedObjects {
        controller.router.addLifecycleListener(this)
        return retainedObjects.getOrPut(controller.instanceId) { RetainedObjects() }
    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.retained.RetainedObjectsHolder"

        internal fun get(controller: Controller): RetainedObjects {
            val activity = (controller.activity as? FragmentActivity)
                ?: error("controller is not attached to a FragmentActivity")
            return ((activity as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(FRAGMENT_TAG) as? RetainedObjectsHolder
                ?: RetainedObjectsHolder().also {
                activity.supportFragmentManager.beginTransaction()
                    .add(
                        it,
                        FRAGMENT_TAG
                    )
                    .commitNow()
            }).getRetainedObjects(controller)
        }

    }
}