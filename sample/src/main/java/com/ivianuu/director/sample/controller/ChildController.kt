package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.bundleOf
import kotlinx.android.synthetic.main.controller_child.tv_title

class ChildController : BaseController() {

    override val layoutRes get() = R.layout.controller_child

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedViewState).apply {
            tv_title.text = args.getString(KEY_TITLE)

            var bgColor = args.getInt(KEY_BG_COLOR)
            if (args.getBoolean(KEY_COLOR_IS_RES)) {
                bgColor = ContextCompat.getColor(context, bgColor)
            }

            setBackgroundColor(bgColor)
        }
    }

    companion object {
        private const val KEY_TITLE = "ChildController.title"
        private const val KEY_BG_COLOR = "ChildController.bgColor"
        private const val KEY_COLOR_IS_RES = "ChildController.colorIsResId"

        fun newInstance(title: String, bgColor: Int, colorIsRes: Boolean): ChildController {
            return ChildController().apply {
                args = bundleOf(
                    KEY_TITLE to title,
                    KEY_BG_COLOR to bgColor,
                    KEY_COLOR_IS_RES to colorIsRes
                )
            }
        }
    }
}