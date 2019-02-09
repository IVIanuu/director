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
import android.view.View
import com.ivianuu.director.Router
import com.ivianuu.director.hasRootController
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import com.ivianuu.director.viewpager2.RouterAdapter
import kotlinx.android.synthetic.main.controller_pager2.view_pager
import java.util.*

class Pager2Controller : BaseController() {

    override val layoutRes get() = R.layout.controller_pager2

    private val pagerAdapter by lazy {
        object : RouterAdapter(childRouterManager) {

            override fun configureRouter(router: Router, position: Int) {
                if (!router.hasRootController) {
                    router.setRoot(
                        ChildController.newInstance(
                            String.format(
                                Locale.getDefault(),
                                "Child #%d (Swipe to see more)",
                                position
                            ), PAGE_COLORS[position], true
                        ).toTransaction()
                    )
                }
            }

            override fun getItemCount(): Int = PAGE_COLORS.size

            // todo override fun getPageTitle(position: Int) = "Page $position"

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "ViewPager 2 Demo"
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        view_pager.adapter = pagerAdapter
        // todo tab_layout.setupWithViewPager(view_pager)
    }

    override fun onUnbindView(view: View) {
        //view_pager.adapter = null
        //tab_layout.setupWithViewPager(null)
        super.onUnbindView(view)
    }

    private companion object {
        private val PAGE_COLORS = intArrayOf(
            R.color.green_300,
            R.color.cyan_300,
            R.color.deep_purple_300,
            R.color.lime_300,
            R.color.red_300
        )
    }

}