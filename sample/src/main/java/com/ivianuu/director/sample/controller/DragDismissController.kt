package com.ivianuu.director.sample.controller

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.view.View
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback

@TargetApi(VERSION_CODES.LOLLIPOP)
class DragDismissController : BaseController() {

    override val layoutRes get() = R.layout.controller_drag_dismiss

    private val dragDismissListener = object : ElasticDragDismissCallback() {
        override fun onDragDismissed() {
            overriddenPopHandler = ScaleFadeChangeHandler()
            router.popController(this@DragDismissController)
        }
    }

    override fun onCreate() {
        super.onCreate()
        title = "Drag to Dismiss"
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)

        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
    }
}