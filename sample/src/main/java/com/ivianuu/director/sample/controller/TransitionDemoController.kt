package com.ivianuu.director.sample.controller

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.ivianuu.director.*
import com.ivianuu.director.common.changehandler.CircularRevealChangeHandler
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.common.changehandler.VerticalChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ArcFadeMoveChangeHandler
import com.ivianuu.director.sample.changehandler.FlipChangeHandler
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.ARC_FADE
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.ARC_FADE_RESET
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.CIRCULAR
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.FADE
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.FLIP
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.HORIZONTAL
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.VERTICAL
import com.ivianuu.director.sample.controller.TransitionDemoController.TransitionDemo.values
import com.ivianuu.director.sample.util.bundleOf
import kotlinx.android.synthetic.main.controller_transition_demo.bg_view
import kotlinx.android.synthetic.main.controller_transition_demo.btn_next
import kotlinx.android.synthetic.main.controller_transition_demo.transition_root
import kotlinx.android.synthetic.main.controller_transition_demo.tv_title

class TransitionDemoController : BaseController() {

    override val layoutRes: Int
        get() = transitionDemo.layoutId
    override val toolbarTitle: String?
        get() = "Transition Demos"

    private val transitionDemo by lazy { TransitionDemo.fromIndex(args.getInt(KEY_INDEX)) }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)
        if (transitionDemo.colorId != 0 && bg_view != null) {
            bg_view.setBackgroundColor(
                ContextCompat.getColor(
                    activity,
                    transitionDemo.colorId
                )
            )
        }

        val nextIndex = transitionDemo.ordinal + 1
        var buttonColor = 0
        if (nextIndex < values().size) {
            buttonColor = TransitionDemo.fromIndex(nextIndex).colorId
        }
        if (buttonColor == 0) {
            buttonColor = TransitionDemo.fromIndex(0).colorId
        }

        btn_next.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(activity, buttonColor))
        tv_title.text = transitionDemo.title

        btn_next.setOnClickListener {
            if (nextIndex < values().size) {
                router.push(
                    getNextTransaction(nextIndex, this@TransitionDemoController)
                )
            } else {
                router.popToRoot()
            }
        }
    }

    fun getChangeHandler(from: Controller): ControllerChangeHandler? = when (transitionDemo) {
        VERTICAL -> VerticalChangeHandler()
        CIRCULAR -> {
            val demoController = from as TransitionDemoController
            CircularRevealChangeHandler(
                demoController.btn_next,
                demoController.transition_root
            )
        }
        FADE -> FadeChangeHandler()
        FLIP -> FlipChangeHandler()
        ARC_FADE -> ArcFadeMoveChangeHandler(
            from.resources.getString(R.string.transition_tag_dot),
            from.resources.getString(R.string.transition_tag_title)
        )
        ARC_FADE_RESET -> ArcFadeMoveChangeHandler(
            from.resources.getString(R.string.transition_tag_dot),
            from.resources.getString(R.string.transition_tag_title)
        )
        HORIZONTAL -> HorizontalChangeHandler()
    }

    enum class TransitionDemo(
        val title: String,
        val layoutId: Int,
        val colorId: Int
    ) {
        VERTICAL(
            "Vertical Slide Animation",
            R.layout.controller_transition_demo,
            R.color.blue_grey_300
        ),
        CIRCULAR(
            "Circular Reveal Animation (on Lollipop and above, else Fade)",
            R.layout.controller_transition_demo,
            R.color.red_300
        ),
        FADE("Fade Animation", R.layout.controller_transition_demo, R.color.blue_300),
        FLIP("Flip Animation", R.layout.controller_transition_demo, R.color.deep_orange_300),
        HORIZONTAL(
            "Horizontal Slide Animation",
            R.layout.controller_transition_demo,
            R.color.green_300
        ),
        ARC_FADE(
            "Arc/Fade Shared Element Transition (on Lollipop and above, else Fade)",
            R.layout.controller_transition_demo_shared,
            0
        ),
        ARC_FADE_RESET(
            "Arc/Fade Shared Element Transition (on Lollipop and above, else Fade)",
            R.layout.controller_transition_demo,
            R.color.pink_300
        );

        companion object {
            fun fromIndex(index: Int) = values()[index]
        }
    }

    companion object {
        private const val KEY_INDEX = "TransitionDemoController.index"

        fun newInstance(index: Int) = TransitionDemoController().apply {
            args = bundleOf(KEY_INDEX to index)
        }

        fun getNextTransaction(index: Int, fromController: Controller): RouterTransaction {
            val toController = newInstance(index)
            return toController
                .toTransaction()
                .changeHandler(toController.getChangeHandler(fromController))
        }
    }
}