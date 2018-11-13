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

package com.ivianuu.director.common

import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

/**
 * A [LayoutContainer] for [Controller]'s
 */
class ControllerLayoutContainer(private val controller: Controller) : LayoutContainer {

    override val containerView: View?
        get() = controller.view

    init {
        controller.addLifecycleListener(object : ControllerLifecycleListener {
            override fun postDestroyView(controller: Controller) {
                super.postDestroyView(controller)
                clearFindViewByIdCache()
            }
        })
    }
}

/**
 * Returns a new [ControllerLayoutContainer] for [this]
 */
fun Controller.ControllerLayoutContainer() = ControllerLayoutContainer(this)