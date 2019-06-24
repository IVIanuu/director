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

package com.ivianuu.director.sample.controller

import com.ivianuu.director.childRouter
import com.ivianuu.director.sample.R
import com.ivianuu.director.traveler.ControllerNavigator
import com.ivianuu.traveler.android.AndroidRouter
import com.ivianuu.traveler.setRoot

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TravelerController : BaseController() {

    override val layoutRes: Int
        get() = R.layout.controller_traveler
    override val toolbarTitle: String?
        get() = "Traveler Demo"

    private val childRouter by childRouter(R.id.traveler_container)

    val travelerRouter by lazy {
        AndroidRouter().apply {
            setNavigator(ControllerNavigator(childRouter))
        }
    }

    override fun onCreate() {
        super.onCreate()
        travelerRouter.setRoot(
            NavigationControllerKey(
                0, NavigationController.DisplayUpMode.HIDE,
                true, NavigationController.AnimMode.HORIZONTAL
            )
        )
    }
}