package com.ivianuu.director.sample.controller

import android.view.View
import com.ivianuu.director.Router
import com.ivianuu.director.internal.d
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import com.ivianuu.director.viewpager.RouterPagerAdapter
import kotlinx.android.synthetic.main.controller_bottom_nav.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class BottomNavController : BaseController() {

    override val layoutRes get() = R.layout.controller_bottom_nav

    private val pagerAdapter = object : RouterPagerAdapter(this@BottomNavController) {

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController) {
                router.setRoot(
                    NavigationDemoController
                        .newInstance(1, NavigationDemoController.DisplayUpMode.HIDE)
                        .toTransaction()
                )
            }
        }

        override fun getPageTitle(position: Int) = "Page $position"

        override fun getCount() = PAGE_COLORS.size
    }

    override fun onCreate() {
        super.onCreate()
        title = "Bottom Nav Demo"
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        view_pager.adapter = pagerAdapter

        bottom_nav_view.setOnNavigationItemSelectedListener { item ->
            val i = (0 until bottom_nav_view.menu.size())
                .map { bottom_nav_view.menu.getItem(it) }
                .indexOfFirst { it == item }

            view_pager.currentItem = i

            true
        }

        bottom_nav_view.setOnNavigationItemReselectedListener { item ->
            val i = (0 until bottom_nav_view.menu.size())
                .map { bottom_nav_view.menu.getItem(it) }
                .indexOfFirst { it == item }

            if (i != -1) {
                pagerAdapter.getRouter(i)!!.popToRoot()
            }
        }
    }

    override fun onDestroyView(view: View) {
        if (requireActivity().isChangingConfigurations) {
            view_pager.adapter = null
        }

        super.onDestroyView(view)
    }

    override fun handleBack(): Boolean {
        return if (view_pager.currentItem != 0) {
            d { "not first item" }
            val router = pagerAdapter.getRouter(view_pager.currentItem)
            if (router!!.backstack.size == 1) {
                d { "router size is one" }
                view_pager.currentItem = 0
                bottom_nav_view.selectedItemId =
                        bottom_nav_view.menu.getItem(view_pager.currentItem).itemId
                true
            } else {
                super.handleBack()
            }
        } else {
            super.handleBack()
        }
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