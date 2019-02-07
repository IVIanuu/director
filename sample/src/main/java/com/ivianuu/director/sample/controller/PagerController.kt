package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import com.ivianuu.director.Router
import com.ivianuu.director.hasRootController
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.viewpager.RouterPagerAdapter
import kotlinx.android.synthetic.main.controller_pager.tab_layout
import kotlinx.android.synthetic.main.controller_pager.view_pager
import java.util.*

class PagerController : BaseController() {

    override val layoutRes get() = R.layout.controller_pager

    private val pagerAdapter by lazy {
        object : RouterPagerAdapter(childRouterManager) {

            override fun configureRouter(router: Router, position: Int) {
                if (!router.hasRootController) {
                    router.setRoot(
                        ChildController.newInstance(
                            String.format(
                                Locale.getDefault(),
                                "Child #%d (Swipe to see more)",
                                position
                            ), PAGE_COLORS[position], true
                        )
                    )
                }
            }

            override fun getPageTitle(position: Int) = "Page $position"

            override fun getCount() = PAGE_COLORS.size
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "ViewPager Demo"
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        view_pager.adapter = pagerAdapter
        tab_layout.setupWithViewPager(view_pager)
    }

    override fun onUnbindView(view: View) {
        view_pager.adapter = null
        tab_layout.setupWithViewPager(null)
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