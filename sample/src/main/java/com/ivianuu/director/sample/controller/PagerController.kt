package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import com.ivianuu.director.Router
import com.ivianuu.director.common.RouterPagerAdapter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_pager.tab_layout
import kotlinx.android.synthetic.main.controller_pager.view_pager
import java.util.*

class PagerController : BaseController() {

    override val layoutRes get() = R.layout.controller_pager
    override val toolbarTitle: String?
        get() = "ViewPager Demo"

    private val pagerAdapter by lazy {
        object : RouterPagerAdapter(childRouterManager) {

            override fun configureRouter(router: Router, position: Int) {
                if (!router.hasRoot) {
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

            override fun getPageTitle(position: Int) = "Page $position"

            override fun getCount() = PAGE_COLORS.size
        }
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

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