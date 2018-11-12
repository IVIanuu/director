package com.ivianuu.director.sample.controller

import android.view.View
import com.ivianuu.director.Router
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import com.ivianuu.director.viewpager.RouterPagerAdapter
import kotlinx.android.synthetic.main.controller_pager.*
import java.util.*

class PagerController : BaseController() {

    override var title: String?
        get() = "ViewPager Demo"
        set(value) {
            super.title = value
        }

    override val layoutRes = R.layout.controller_pager

    private val pagerAdapter = object : RouterPagerAdapter(this@PagerController) {

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController) {
                val page = ChildController.newInstance(
                    String.format(
                        Locale.getDefault(),
                        "Child #%d (Swipe to see more)",
                        position
                    ), PAGE_COLORS[position], true
                )

                router.setRoot(page.toTransaction())
            }
        }

        override fun getPageTitle(position: Int) = "Page $position"

        override fun getCount() = PAGE_COLORS.size
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        view_pager.adapter = pagerAdapter
        tab_layout.setupWithViewPager(view_pager)
    }

    override fun onDestroyView(view: View) {
        if (requireActivity().isChangingConfigurations) {
            view_pager.adapter = null
        }
        tab_layout.setupWithViewPager(null)

        super.onDestroyView(view)
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