package com.ivianuu.director.sample.controllers

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.view.View
import com.ivianuu.director.requireRouter
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback

@TargetApi(VERSION_CODES.LOLLIPOP)
class DragDismissController : BaseController() {

    private val dragDismissListener = object : ElasticDragDismissCallback() {
        override fun onDragDismissed() {
            overriddenPopHandler = ScaleFadeChangeHandler()
            requireRouter().popController(this@DragDismissController)
        }
    }

    override var title: String?
        get() = "Drag to Dismiss"
        set(value) { super.title = value }

    override val layoutRes = R.layout.controller_drag_dismiss

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)

        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
    }
}