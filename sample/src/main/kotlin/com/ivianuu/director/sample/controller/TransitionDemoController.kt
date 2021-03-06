/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director.sample.controller

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.RouterTransaction
import com.ivianuu.director.changeHandler
import com.ivianuu.director.common.changehandler.CircularRevealChangeHandler
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.common.changehandler.VerticalChangeHandler
import com.ivianuu.director.popToRoot
import com.ivianuu.director.push
import com.ivianuu.director.requireActivity
import com.ivianuu.director.requireView

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
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_transition_demo.*

class TransitionDemoController(
    private val index: Int
) : BaseController() {

    override val layoutRes: Int
        get() = transitionDemo.layoutId
    override val toolbarTitle: String?
        get() = "Transition Demos"

    private val transitionDemo by lazy { TransitionDemo.fromIndex(index) }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        if (transitionDemo.colorId != 0 && bg_view != null) {
            bg_view.setBackgroundColor(
                ContextCompat.getColor(
                    requireActivity(),
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
            ColorStateList.valueOf(ContextCompat.getColor(requireActivity(), buttonColor))
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
            listOf(
                from.requireView().resources.getString(R.string.transition_tag_dot),
                from.requireView().resources.getString(R.string.transition_tag_title)
            )
        )
        ARC_FADE_RESET -> ArcFadeMoveChangeHandler(
            listOf(
                from.requireView().resources.getString(R.string.transition_tag_dot),
                from.requireView().resources.getString(R.string.transition_tag_title)
            )
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
        fun getNextTransaction(index: Int, fromController: Controller): RouterTransaction {
            val toController = TransitionDemoController(index)
            return toController
                .toTransaction()
                .changeHandler(toController.getChangeHandler(fromController))
        }
    }
}