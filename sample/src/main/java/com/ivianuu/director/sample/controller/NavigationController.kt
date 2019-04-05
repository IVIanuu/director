package com.ivianuu.director.sample.controller


import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.ivianuu.director.*
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.common.changehandler.VerticalChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.ColorUtil
import com.ivianuu.director.sample.util.bundleOf
import com.ivianuu.director.traveler.ControllerKey
import com.ivianuu.traveler.Command
import com.ivianuu.traveler.navigate
import com.ivianuu.traveler.popToRoot
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.controller_navigation.btn_next
import kotlinx.android.synthetic.main.controller_navigation.btn_pop_to_root
import kotlinx.android.synthetic.main.controller_navigation.btn_up
import kotlinx.android.synthetic.main.controller_navigation.tv_title

class NavigationController : BaseController() {

    override val layoutRes get() = R.layout.controller_navigation

    private val index by lazy { args.getInt(KEY_INDEX) }
    private val displayUpMode by lazy { args.getParcelable<DisplayUpMode>(KEY_DISPLAY_UP_MODE)!! }
    private val useTraveler by lazy { args.getBoolean(KEY_USE_TRAVELER) }
    private val animMode by lazy { args.getParcelable<AnimMode>(KEY_ANIM_MODE)!! }

    private val travelerRouter get() = (parentController as TravelerController).travelerRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbarTitle = "Navigation Demos"
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        if (displayUpMode != DisplayUpMode.SHOW) {
            btn_up.visibility = View.GONE
        }

        view.setBackgroundColor(ColorUtil.getMaterialColor(resources, index))
        tv_title.text = resources.getString(R.string.navigation_title, index)

        btn_next.setOnClickListener {
            if (useTraveler) {
                travelerRouter.navigate(
                    NavigationControllerKey(
                        index + 1,
                        displayUpMode.displayUpModeForChild,
                        useTraveler,
                        animMode
                    )
                )
            } else {
                stackRouter.push(
                    NavigationController.newInstance(
                        index + 1,
                        displayUpMode.displayUpModeForChild,
                        useTraveler,
                        animMode
                    ).toTransaction()
                        .changeHandler(animMode.createHandler())
                )
            }
        }

        btn_up.setOnClickListener {
            if (useTraveler) {
                // not required
            } else {
                stackRouter.popToTag(TAG_UP_TRANSACTION)
            }
        }

        btn_pop_to_root.setOnClickListener {
            if (useTraveler) {
                travelerRouter.popToRoot()
            } else {
                stackRouter.popToRoot()
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        if (view == null) return
        btn_next.isEnabled = enabled
        btn_up.isEnabled = enabled
        btn_pop_to_root.isEnabled = enabled
    }

    @Parcelize
    enum class DisplayUpMode : Parcelable {
        SHOW,
        SHOW_FOR_CHILDREN_ONLY,
        HIDE;

        val displayUpModeForChild: DisplayUpMode
            get() = when (this) {
                HIDE -> HIDE
                else -> SHOW
            }
    }

    @Parcelize
    enum class AnimMode : Parcelable {
        HORIZONTAL {
            override fun createHandler(): ChangeHandler =
                HorizontalChangeHandler()
        },
        VERTICAL {
            override fun createHandler(): ChangeHandler =
                VerticalChangeHandler()
        };

        abstract fun createHandler(): ChangeHandler
    }

    companion object {
        const val TAG_UP_TRANSACTION = "NavigationController.up"
        private const val KEY_INDEX = "NavigationController.index"
        private const val KEY_DISPLAY_UP_MODE = "NavigationController.displayUpMode"
        private const val KEY_ANIM_MODE = "NavigationController.animMode"
        private const val KEY_USE_TRAVELER = "NavigationController.useTraveler"

        fun newInstance(
            index: Int,
            displayUpMode: DisplayUpMode,
            useTraveler: Boolean = false,
            animMode: AnimMode = AnimMode.HORIZONTAL
        ) = NavigationController().apply {
            args = bundleOf(
                KEY_INDEX to index,
                KEY_DISPLAY_UP_MODE to displayUpMode,
                KEY_USE_TRAVELER to useTraveler,
                KEY_ANIM_MODE to animMode
            )
        }
    }
}

data class NavigationControllerKey(
    val index: Int,
    val displayUpMode: NavigationController.DisplayUpMode,
    val useTraveler: Boolean,
    val animMode: NavigationController.AnimMode
) : ControllerKey {

    override fun createController(data: Any?) = NavigationController.newInstance(
        index, displayUpMode, useTraveler, animMode
    )

    override fun setupTransaction(
        command: Command,
        currentController: Controller?,
        nextController: Controller,
        transaction: Transaction
    ) {
        transaction.changeHandler(animMode.createHandler())
    }

}