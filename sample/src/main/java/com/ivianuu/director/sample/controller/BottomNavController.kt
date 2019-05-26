package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import com.ivianuu.director.*
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.SingleContainer
import com.ivianuu.director.sample.util.setByTag
import kotlinx.android.synthetic.main.controller_bottom_nav.bottom_nav_view

/**
 * @author Manuel Wrage (IVIanuu)
 */
class BottomNavController : BaseController() {

    override val layoutRes get() = R.layout.controller_bottom_nav
    override val toolbarTitle: String?
        get() = "Bottom Nav Demo"

    private var currentIndex = -1

    private val bottomNavContainer by lazy {
        SingleContainer(getChildRouter(R.id.bottom_nav_container))
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)
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
                bottomNavContainer.currentTransaction
                    ?.controller
                    ?.childRouters
                    ?.first()
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
            val currentChildRouter = currentController.childRouters.first()

            if (currentChildRouter.backstackSize == 1) {
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
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

            BottomNavChildController.newInstance(startIndex)
                .toTransaction()
                .changeHandler(FadeChangeHandler())
                .tag(index.toString())
        }

        currentIndex = index
    }

    private companion object {
        private const val KEY_CURRENT_INDEX = "BottomNavController.currentIndex"
    }
}