package com.ivianuu.director.sample.controller

import android.view.View
import com.ivianuu.director.sample.R
import kotlinx.android.synthetic.main.controller_text.*

class TextController(private val text: String) : BaseController() {

    override val layoutRes get() = R.layout.controller_text

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        text_view.text = text
    }

}
