package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.ivianuu.director.Router
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.internal.d
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.controller_bottom_nav.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class BottomNavController : BaseController() {

    override val layoutRes get() = R.layout.controller_bottom_nav

    private val savedStates = mutableMapOf<Int, Bundle>()
    private var currentIndex = -1

    private lateinit var bottomNavRouter: Router

    override fun onCreate() {
        super.onCreate()
        title = "Bottom Nav Demo"
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        bottomNavRouter = getChildRouter(bottom_nav_container)

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

    /*override fun handleBack(): Boolean {
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
    }*/

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getParcelableArrayList<SavedStateWithIndex>(KEY_SAVED_STATES)!!
            .forEach { savedStates[it.index] = it.state }
        currentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(
            KEY_SAVED_STATES, ArrayList(
                savedStates.entries
                    .map { SavedStateWithIndex(it.key, it.value) }
            ))

        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
    }

    private fun swapTo(index: Int) {
        d { "swap to $index" }
        val currentController = bottomNavRouter.backstack.firstOrNull()?.controller

        d { "current controller $currentController" }

        if (currentController != null) {
            val savedState = bottomNavRouter.saveControllerInstanceState(currentController)

            d { "saved current state $savedState" }

            savedStates[currentIndex] = savedState
        }

        val newController = BottomNavChildController()

        val savedState = savedStates[index]

        d { "saved state of the new controller $savedState" }

        if (savedState != null) {
            newController.setInitialSavedState(savedState)
        }

        bottomNavRouter.setRoot(
            newController.toTransaction()
                .pushChangeHandler(FadeChangeHandler())
                .popChangeHandler(FadeChangeHandler())
        )

        currentIndex = index
    }

    @Parcelize
    private data class SavedStateWithIndex(val index: Int, val state: Bundle) : Parcelable

    private companion object {
        private const val KEY_SAVED_STATES = "saved_states"
        private const val KEY_CURRENT_INDEX = "current_index"
    }
}