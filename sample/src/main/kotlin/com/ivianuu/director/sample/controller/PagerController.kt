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
import com.ivianuu.director.Router
import com.ivianuu.director.childRouter
import com.ivianuu.director.common.RouterPagerAdapter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_pager.*
import java.util.*

class PagerController : BaseController() {

    override val layoutRes get() = R.layout.controller_pager
    override val toolbarTitle: String?
        get() = "ViewPager Demo"

    private val pagerAdapter by lazy {
        object : RouterPagerAdapter({ childRouter() }) {

            override fun configureRouter(router: Router, position: Int) {
                if (!router.hasRoot) {
                    router.setRoot(
                        ChildController(
                            String.format(
                                Locale.getDefault(),
                                "Child #%d (Swipe to see more)",
                                position
                            ), PAGE_COLORS[position], true
                        ).toTransaction()
                    )
                }
            }

            override fun getPageTitle(position: Int) = "Page $position"

            override fun getCount() = PAGE_COLORS.size
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        view_pager.adapter = pagerAdapter
        tab_layout.setupWithViewPager(view_pager)
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