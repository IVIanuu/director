package com.ivianuu.director.sample.controller

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.pop
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback

@TargetApi(VERSION_CODES.LOLLIPOP)
class DragDismissController : BaseController() {

    override val layoutRes get() = R.layout.controller_drag_dismiss

    private val dragDismissListener = object : ElasticDragDismissCallback {
        override fun onDragDismissed() {
            (view as ElasticDragDismissFrameLayout).removeListener(this)
            router.pop(this@DragDismissController, ScaleFadeChangeHandler())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Drag to Dismiss"
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return super.onInflateView(inflater, container, savedViewState).apply {
            translationY = 1000f
        }
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
    }

    override fun onUnbindView(view: View) {
        super.onUnbindView(view)

        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
    }
}