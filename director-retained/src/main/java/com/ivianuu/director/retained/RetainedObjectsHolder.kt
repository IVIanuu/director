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

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivianuu.director.Controller
import com.ivianuu.director.activity
import com.ivianuu.director.doOnPostDestroy

/**
 * Holds retained objects of a [Controller]
 */
internal class RetainedObjectsHolder : ViewModel() {

    private val retainedObjects =
        mutableMapOf<String, RetainedObjects>()

    internal fun getRetainedObjects(controller: Controller): RetainedObjects {
        controller.removeRetainedObjectsOnPostDestroy()
        return retainedObjects.getOrPut(controller.instanceId) { RetainedObjects() }
    }

    private fun Controller.removeRetainedObjectsOnPostDestroy() {
        doOnPostDestroy {
            if (!activity.isChangingConfigurations) {
                this@RetainedObjectsHolder.retainedObjects.remove(instanceId)
            }
        }
    }

    private object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return RetainedObjectsHolder() as T
        }
    }

    companion object {
        internal fun get(controller: Controller): RetainedObjects {
            val activity = (controller.activity as? FragmentActivity)
                ?: error("controller is not attached to a FragmentActivity")
            return ViewModelProvider(activity, Factory)
                .get(RetainedObjectsHolder::class.java).getRetainedObjects(controller)
        }

    }
}