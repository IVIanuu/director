package com.ivianuu.director.sample.controller

import android.view.View
import androidx.core.content.ContextCompat
import com.ivianuu.director.sample.R

import kotlinx.android.synthetic.main.controller_child.*

class ChildController(
    private val title: String,
    private val bgColor: Int,
    private val colorIsRes: Boolean
) : BaseController() {

    override val layoutRes get() = R.layout.controller_child

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        tv_title.text = title

        var bgColor = this.bgColor
        if (colorIsRes) {
            bgColor = ContextCompat.getColor(activity, bgColor)
        }

        view.setBackgroundColor(bgColor)
    }

}