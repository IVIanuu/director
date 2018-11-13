package com.ivianuu.director.sample.controller

import android.view.View
import androidx.core.content.ContextCompat

import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.bundleOf
import kotlinx.android.synthetic.main.controller_child.*

class ChildController : BaseController() {

    override val layoutRes get() = R.layout.controller_child

    override fun onBindView(view: View) {
        super.onBindView(view)

        tv_title.text = args.getString(KEY_TITLE)

        var bgColor = args.getInt(KEY_BG_COLOR)
        if (args.getBoolean(KEY_COLOR_IS_RES)) {
            bgColor = ContextCompat.getColor(activity, bgColor)
        }

        view.setBackgroundColor(bgColor)
    }

    companion object {
        private const val KEY_TITLE = "ChildController.title"
        private const val KEY_BG_COLOR = "ChildController.bgColor"
        private const val KEY_COLOR_IS_RES = "ChildController.colorIsResId"

        fun newInstance(title: String, bgColor: Int, colorIsRes: Boolean) = ChildController().apply {
            args = bundleOf(
                KEY_TITLE to title,
                KEY_BG_COLOR to bgColor,
                KEY_COLOR_IS_RES to colorIsRes
            )
        }
    }
}