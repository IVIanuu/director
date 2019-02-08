package com.ivianuu.director.sample.controller

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.pop
import com.ivianuu.director.replaceTop
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.util.d
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback
import kotlinx.android.synthetic.main.controller_drag_dismiss.header
import kotlinx.android.synthetic.main.controller_drag_dismiss.swap

@TargetApi(VERSION_CODES.LOLLIPOP)
class DragDismissController : BaseController() {

    override val layoutRes get() = R.layout.controller_drag_dismiss

    private val dragDismissListener = object : ElasticDragDismissCallback() {
        override fun onDragDismissed() {
            (view as ElasticDragDismissFrameLayout).removeListener(this)
            router.pop(this@DragDismissController, ScaleFadeChangeHandler())
        }
    }

    private val removesFromViewOnPush by lazy {
        val transaction = router.backstack.first { it.controller == this }
        transaction.pushChangeHandler!!.removesFromViewOnPush
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Drag to Dismiss"
        d { "on create -> removes on push $removesFromViewOnPush" }
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)

        header.setBackgroundColor(HomeItem.values().toList().shuffled().first().color)

        swap.setOnClickListener {
            d { "click" }
            router.replaceTop(
                DragDismissController(), HorizontalChangeHandler(
                    removesFromViewOnPush = !removesFromViewOnPush,
                    duration = 500
                )
            )
        }
    }

    override fun toString(): String {
        return "Drag dismiss removes from view $removesFromViewOnPush"
    }

    override fun onUnbindView(view: View) {
        super.onUnbindView(view)

        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
    }
}