package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.common.changehandler.fade
import com.ivianuu.director.popToRoot
import com.ivianuu.director.sample.R
import com.ivianuu.director.setBackstack
import com.ivianuu.director.transaction
import kotlinx.android.synthetic.main.controller_bottom_nav.bottom_nav_view
import java.util.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class BottomNavController : BaseController() {

    override val layoutRes get() = R.layout.controller_bottom_nav

    private var currentIndex = -1

    private val bottomNavRouter by lazy {
        getChildRouter(R.id.bottom_nav_container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Bottom Nav Demo"

        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX)
        }
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)

        bottom_nav_view.setOnNavigationItemSelectedListener { item ->
            val i = (0 until bottom_nav_view.menu.size())
                .map { bottom_nav_view.menu.getItem(it) }
                .indexOfFirst { it == item }

            swapTo(i)

            true
        }

        bottom_nav_view.setOnNavigationItemReselectedListener { item ->
            val i = (0 until bottom_nav_view.menu.size())
                .map { bottom_nav_view.menu.getItem(it) }
                .indexOfFirst { it == item }

            if (i != -1) {
                // you would probably use a interface in a production app
                (bottomNavRouter.backstack
                    .first()
                    .controller as BottomNavChildController)
                    .childRouters
                    .first()
                    .popToRoot(FadeChangeHandler())
            }
        }

        if (currentIndex == -1) {
            swapTo(0)
        }
    }

    override fun handleBack(): Boolean {
        val currentController = bottomNavRouter.backstack.last().controller

        if (currentController.handleBack()) {
            return true
        }

        return if (currentIndex != 0) {
            val router = (bottomNavRouter.backstack
                .first()
                .controller as BottomNavChildController)
                .childRouters
                .first()

            if (router.backstack.size == 1) {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
    }

    private fun swapTo(index: Int) {
        bottomNavRouter.setBackstack {
            val backstackIndex = indexOfFirst { it.tag == index.toString() }

            if (backstackIndex != -1) {
                Collections.swap(this, backstackIndex, lastIndex)
            } else {
                add(
                    transaction {
                        controller(BottomNavController())
                        fade()
                        tag(index.toString())
                    }
                )
            }

        }
        currentIndex = index
    }

    private companion object {
        private const val KEY_CURRENT_INDEX = "BottomNavController.currentIndex"
    }
}