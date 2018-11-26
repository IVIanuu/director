package com.ivianuu.director.sample

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.common.changehandler.VerticalChangeHandler
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.popCurrentController
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.pushController
import com.ivianuu.director.toTransaction

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TestControllerOne : Controller() {

    private var launched = false
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        router.popCurrentController()
    }

    override fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(changeHandler, changeType)
        d { "on change ended -> $changeType" }
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            addView(
                View(activity).apply {
                    setBackgroundColor(Color.RED)
                    setOnClickListener {
                        launched = true
                        router.pushController(
                            TestControllerTwo().toTransaction()
                                .pushChangeHandler(VerticalChangeHandler())
                                .popChangeHandler(VerticalChangeHandler())
                        )
                    }
                },
                FrameLayout.LayoutParams(400, 400).apply {
                    gravity = Gravity.CENTER
                }
            )
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        /*if (launched)
        router.pushController(
            BottomNavController().toTransaction()
                .pushChangeHandler(FadeChangeHandler())
                .popChangeHandler(FadeChangeHandler())
        )

        launched = false

        if (!started) {
            d { "joo" }
            started = true
            router.pushController(
                TravelerController().toTransaction()
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
            )
        }*/
    }
}

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TestControllerTwo : Controller() {

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return FrameLayout(activity).apply {
            setBackgroundColor(Color.BLUE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { router.popCurrentController() }
        }
    }

}