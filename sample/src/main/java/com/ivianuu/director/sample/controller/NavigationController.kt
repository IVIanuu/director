package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.RouterTransaction
import com.ivianuu.director.changeHandler
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.popToRoot
import com.ivianuu.director.popToTag
import com.ivianuu.director.pushController
import com.ivianuu.director.resources
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.ColorUtil
import com.ivianuu.director.sample.util.bundleOf
import com.ivianuu.director.toTransaction
import com.ivianuu.director.traveler.ControllerKey
import com.ivianuu.traveler.Command
import com.ivianuu.traveler.navigate
import com.ivianuu.traveler.popToRoot
import kotlinx.android.synthetic.main.controller_navigation.btn_next
import kotlinx.android.synthetic.main.controller_navigation.btn_pop_to_root
import kotlinx.android.synthetic.main.controller_navigation.btn_up
import kotlinx.android.synthetic.main.controller_navigation.tv_title
import kotlinx.android.synthetic.main.controller_navigation.view.btn_up

class NavigationController : BaseController() {

    override val layoutRes get() = R.layout.controller_navigation

    private val index by lazy { args.getInt(KEY_INDEX) }
    private val displayUpMode by lazy { DisplayUpMode.values()[args.getInt(KEY_DISPLAY_UP_MODE)] }
    private val useTraveler by lazy { args.getBoolean(KEY_USE_TRAVELER) }

    private val travelerRouter get() = (parentController as TravelerController).travelerRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Navigation Demos"
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)

        if (displayUpMode != DisplayUpMode.SHOW) {
            view.btn_up.visibility = View.GONE
        }

        view.setBackgroundColor(ColorUtil.getMaterialColor(resources, index))
        tv_title.text = resources.getString(R.string.navigation_title, index)

        btn_next.setOnClickListener {
            if (useTraveler) {
                travelerRouter.navigate(
                    NavigationControllerKey(
                        index + 1,
                        displayUpMode.displayUpModeForChild,
                        useTraveler
                    )
                )
            } else {
                router.pushController(
                    NavigationController.newInstance(
                        index + 1,
                        displayUpMode.displayUpModeForChild,
                        useTraveler
                    )
                        .toTransaction()
                        .changeHandler(HorizontalChangeHandler())
                )
            }
        }

        btn_up.setOnClickListener {
            if (useTraveler) {
                // not required
            } else {
                router.popToTag(TAG_UP_TRANSACTION)
            }
        }

        btn_pop_to_root.setOnClickListener {
            if (useTraveler) {
                travelerRouter.popToRoot()
            } else {
                router.popToRoot()
            }
        }
    }

    override fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeStarted(changeHandler, changeType)
        setButtonsEnabled(false)
    }

    override fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(changeHandler, changeType)
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        if (view == null) return
        btn_next.isEnabled = enabled
        btn_up.isEnabled = enabled
        btn_pop_to_root.isEnabled = enabled
    }

    enum class DisplayUpMode {
        SHOW,
        SHOW_FOR_CHILDREN_ONLY,
        HIDE;

        val displayUpModeForChild: DisplayUpMode
            get() = when (this) {
                HIDE -> HIDE
                else -> SHOW
            }
    }

    companion object {
        const val TAG_UP_TRANSACTION = "NavigationController.up"
        private const val KEY_INDEX = "NavigationController.index"
        private const val KEY_DISPLAY_UP_MODE = "NavigationController.displayUpMode"
        private const val KEY_USE_TRAVELER = "NavigationController.useTraveler"

        fun newInstance(
            index: Int,
            displayUpMode: DisplayUpMode,
            useTraveler: Boolean
        ) = NavigationController().apply {
            args = bundleOf(
                KEY_INDEX to index,
                KEY_DISPLAY_UP_MODE to displayUpMode.ordinal,
                KEY_USE_TRAVELER to useTraveler
            )
        }
    }
}

data class NavigationControllerKey(
    val index: Int,
    val displayUpMode: NavigationController.DisplayUpMode,
    val useTraveler: Boolean
) : ControllerKey {

    override fun createController(data: Any?) = NavigationController.newInstance(
        index, displayUpMode, useTraveler
    )

    override fun setupTransaction(
        command: Command,
        currentController: Controller?,
        nextController: Controller,
        transaction: RouterTransaction
    ) {
        transaction.changeHandler(HorizontalChangeHandler())
    }
}