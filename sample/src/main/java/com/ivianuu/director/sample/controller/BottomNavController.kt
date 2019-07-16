package com.ivianuu.director.sample.controller

import android.view.View
import com.ivianuu.director.changeHandler
import com.ivianuu.director.childRouter
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.popToRoot
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.SingleContainer
import com.ivianuu.director.sample.util.setByTag
import com.ivianuu.director.tag
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_bottom_nav.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class BottomNavController : BaseController() {

    override val layoutRes get() = R.layout.controller_bottom_nav
    override val toolbarTitle: String?
        get() = "Bottom Nav Demo"

    private var currentIndex = -1

    private val bottomNavContainer by lazy {
        SingleContainer(childRouter(R.id.bottom_nav_container))
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        bottom_nav_view.setOnNavigationItemSelectedListener { item ->
            val i = (0 until bottom_nav_view.menu.size())
                .map { bottom_nav_view.menu.getItem(it) }
                .indexOfFirst { it == item }

            swapTo(i)

            return@setOnNavigationItemSelectedListener true
        }

        bottom_nav_view.setOnNavigationItemReselectedListener { item ->
            val i = (0 until bottom_nav_view.menu.size())
                .map { bottom_nav_view.menu.getItem(it) }
                .indexOfFirst { it == item }

            // pop to root on re selections
            if (i != -1) {
                (bottomNavContainer.currentTransaction
                    ?.controller as? BottomNavChildController)
                    ?.childRouter
                    ?.popToRoot()
            }
        }

        if (currentIndex == -1) {
            swapTo(0)
        }
    }

    override fun handleBack(): Boolean {
        val currentController = bottomNavContainer.currentTransaction?.controller
            ?: return false

        if (currentController.handleBack()) {
            return true
        }

        return if (currentIndex != 0) {
            val currentChildRouter = (bottomNavContainer.currentTransaction
                ?.controller as? BottomNavChildController)!!.childRouter

            if (currentChildRouter.backstack.size == 1) {
                swapTo(0)
                bottom_nav_view.selectedItemId =
                    bottom_nav_view.menu.getItem(currentIndex).itemId
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    private fun swapTo(index: Int) {
        bottomNavContainer.setByTag(index.toString()) {
            val startIndex = when (index) {
                0 -> 5
                1 -> 4
                2 -> 3
                3 -> 2
                4 -> 1
                else -> error("should not happen")
            }

            BottomNavChildController(startIndex)
                .toTransaction()
                .changeHandler(FadeChangeHandler())
                .tag(index.toString())
        }

        currentIndex = index
    }

}