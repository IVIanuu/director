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

import android.view.View
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction

/**
 * @author Manuel Wrage (IVIanuu)
 */
class BottomNavChildController : BaseController() {

    override val layoutRes: Int
        get() = R.layout.controller_bottom_nav_child

    private val childRouter by lazy { getChildRouter(R.id.bottom_nav_child_container) }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        if (!childRouter.hasRootController) {
            childRouter.setRoot(
                NavigationDemoController
                    .newInstance(1, NavigationDemoController.DisplayUpMode.HIDE)
                    .toTransaction()
                    .pushChangeHandler(FadeChangeHandler())
            )
        }
    }
}