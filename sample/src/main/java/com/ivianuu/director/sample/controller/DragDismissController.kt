package com.ivianuu.director.sample.controller

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import com.ivianuu.director.popController
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback

@TargetApi(VERSION_CODES.LOLLIPOP)
class DragDismissController : BaseController() {

    override val layoutRes get() = R.layout.controller_drag_dismiss

    private val dragDismissListener = object : ElasticDragDismissCallback() {
        override fun onDragDismissed() {
            (view as ElasticDragDismissFrameLayout).removeListener(this)
            overriddenPopHandler = ScaleFadeChangeHandler()
            router.popController(this@DragDismissController)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Drag to Dismiss"
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
    }

    override fun onUnbindView(view: View) {
        super.onUnbindView(view)

        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
    }
}