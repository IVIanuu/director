package com.ivianuu.director.sample.controller

import android.view.View
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.bundleOf
import kotlinx.android.synthetic.main.controller_text.*

class TextController : BaseController() {

    override val layoutRes = R.layout.controller_text

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        text_view.text = args.getString(KEY_TEXT)
    }

    companion object {
        private const val KEY_TEXT = "TextController.text"

        fun newInstance(text: String) = TextController().apply {
            args = bundleOf(KEY_TEXT to text)
        }
    }
}
