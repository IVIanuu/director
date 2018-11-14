package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.ivianuu.director.Router
import com.ivianuu.director.common.changehandler.FadeChangeHandler
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
        bottomNavRouter = getChildRouter(R.id.bottom_nav_container)
        actionBarTitle = "Bottom Nav Demo"
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

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
                super.handleBack()
            }
        } else {
            super.handleBack()
        }
    }

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
        val currentController = bottomNavRouter.backstack.firstOrNull()?.controller

        if (currentController != null) {
            val savedState = bottomNavRouter.saveControllerInstanceState(currentController)
            savedStates[currentIndex] = savedState
        }

        val newController = BottomNavChildController()

        val savedState = savedStates[index]

        if (savedState != null) {
            newController.setInitialSavedState(savedState)
        }

        bottomNavRouter.setRoot(newController.toTransaction())

        currentIndex = index
    }

    @Parcelize
    private data class SavedStateWithIndex(val index: Int, val state: Bundle) : Parcelable

    private companion object {
        private const val KEY_SAVED_STATES = "BottomNavController.savedStates"
        private const val KEY_CURRENT_INDEX = "BottomNavController.currentIndex"
    }
}