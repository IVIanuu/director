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

import android.os.Bundle
import com.ivianuu.director.changeHandler
import com.ivianuu.director.childRouter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.push
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

/**
 * @author Manuel Wrage (IVIanuu)
 */
class BottomNavChildController : BaseController() {

    override val layoutRes: Int
        get() = R.layout.controller_bottom_nav_child

    private val childRouter by childRouter(R.id.bottom_nav_child_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!childRouter.hasRoot) {
            (1..args.getInt(KEY_START_INDEX))
                .map {
                    NavigationController.newInstance(
                        it, NavigationController.DisplayUpMode.HIDE, false,
                        NavigationController.AnimMode.VERTICAL
                    ) to it
                }
                .forEach { (controller, i) ->
                    if (i == 1) {
                        childRouter.setRoot(
                            controller.toTransaction()
                                .changeHandler(NavigationController.AnimMode.VERTICAL.createHandler())
                        )
                    } else {
                        childRouter.push(
                            controller.toTransaction()
                                .changeHandler(NavigationController.AnimMode.VERTICAL.createHandler())
                        )
                    }
                }
        }
    }

    companion object {
        private const val KEY_START_INDEX = "start_index"

        fun newInstance(startIndex: Int): BottomNavChildController = BottomNavChildController()
            .apply { args.putInt(KEY_START_INDEX, startIndex) }
    }
}