package com.ivianuu.director.sample.controllers

import android.os.Bundle
import android.view.View
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.common.HorizontalChangeHandler
import com.ivianuu.director.internal.d
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.requireResources
import com.ivianuu.director.requireRouter
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BundleBuilder
import com.ivianuu.director.sample.util.ColorUtil
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_navigation_demo.*
import kotlinx.android.synthetic.main.controller_navigation_demo.view.*

class NavigationDemoController : BaseController() {

    private val index by lazy { args.getInt(KEY_INDEX) }
    private val displayUpMode by lazy { DisplayUpMode.values()[args.getInt(KEY_DISPLAY_UP_MODE)] }

    override var title: String?
        get() = "Navigation Demos"
        set(value) { super.title = value }

    override val layoutRes = R.layout.controller_navigation_demo

    override fun onCreate() {
        super.onCreate()
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        d { "on view created" }

        if (displayUpMode != DisplayUpMode.SHOW) {
            view.btn_up.visibility = View.GONE
        }

        view.setBackgroundColor(ColorUtil.getMaterialColor(requireResources(), index))
        tv_title.text = requireResources().getString(R.string.navigation_title, index)

        btn_next.setOnClickListener {
            requireRouter().pushController(
                NavigationDemoController.newInstance(index + 1, displayUpMode.displayUpModeForChild)
                    .toTransaction()
                    .pushChangeHandler(HorizontalChangeHandler())
                    .popChangeHandler(HorizontalChangeHandler())
            )
        }

        btn_up.setOnClickListener { requireRouter().popToTag(TAG_UP_TRANSACTION) }

        btn_pop_to_root.setOnClickListener { requireRouter().popToRoot() }
    }

    override fun onDestroyView(view: View) {
        d { "on destroy view" }
        super.onDestroyView(view)
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
        const val TAG_UP_TRANSACTION = "NavigationDemoController.up"
        private const val KEY_INDEX = "NavigationDemoController.index"
        private const val KEY_DISPLAY_UP_MODE = "NavigationDemoController.displayUpMode"

        fun newInstance(index: Int, displayUpMode: DisplayUpMode) = NavigationDemoController().apply {
            args = BundleBuilder(Bundle())
                .putInt(KEY_INDEX, index)
                .putInt(KEY_DISPLAY_UP_MODE, displayUpMode.ordinal)
                .build()
        }
    }
}