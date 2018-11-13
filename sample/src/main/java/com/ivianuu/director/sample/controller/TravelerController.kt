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

import com.ivianuu.director.sample.R
import com.ivianuu.director.traveler.ControllerNavigator
import com.ivianuu.traveler.Traveler
import com.ivianuu.traveler.setRoot

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TravelerController : BaseController() {

    override val layoutRes: Int
        get() = R.layout.controller_traveler

    private val childRouter = getChildRouter(R.id.traveler_container)

    private val traveler = Traveler().apply {
        navigatorHolder.setNavigator(ControllerNavigator(childRouter))
    }

    val travelerRouter get() = traveler.router

    override fun onCreate() {
        super.onCreate()
        title = "Traveler Demo"
        if (!childRouter.hasRootController) {
            travelerRouter.setRoot(
                NavigationControllerKey(0, NavigationController.DisplayUpMode.HIDE, false)
            )
        }
    }
}