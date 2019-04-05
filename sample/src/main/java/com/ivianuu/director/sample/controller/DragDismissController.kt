package com.ivianuu.director.sample.controller

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.view.View
import com.ivianuu.director.pop
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback

@TargetApi(VERSION_CODES.LOLLIPOP)
class DragDismissController : BaseController() {

    override val layoutRes get() = R.layout.controller_drag_dismiss
    override val toolbarTitle: String?
        get() = "Drag to Dismiss"

    private val dragDismissListener = object : ElasticDragDismissCallback {
        override fun onDragDismissed() {
            (view as ElasticDragDismissFrameLayout).removeListener(this)
            router.pop(this@DragDismissController, ScaleFadeChangeHandler())
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
    }

    override fun onDetach(view: View) {
        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
        super.onDetach(view)
    }
}