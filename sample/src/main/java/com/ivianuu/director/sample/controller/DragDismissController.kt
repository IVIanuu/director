package com.ivianuu.director.sample.controller

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.popController
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.replaceTopController
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_drag_dismiss.tv_lorem_ipsum

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

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
        tv_lorem_ipsum.setOnClickListener {
            val removesFromViewOnPush = !lastRemovesFromViewOnPush
            lastRemovesFromViewOnPush = removesFromViewOnPush
            router.replaceTopController(
                DragDismissController().toTransaction()
                    .pushChangeHandler(HorizontalChangeHandler(removesFromViewOnPush = removesFromViewOnPush))
                    .popChangeHandler(FadeChangeHandler())
            )
        }
    }

    override fun onUnbindView(view: View) {
        super.onUnbindView(view)

        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
    }
}

private var lastRemovesFromViewOnPush = false