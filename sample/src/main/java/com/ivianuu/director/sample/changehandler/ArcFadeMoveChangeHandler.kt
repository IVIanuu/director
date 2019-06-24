package com.ivianuu.director.sample.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.transition.ArcMotion
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.transition.TransitionSet
import android.view.View
import com.ivianuu.director.ChangeData
import com.ivianuu.director.common.changehandler.SharedElementTransitionChangeHandler
import com.ivianuu.director.common.findNamedView

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ArcFadeMoveChangeHandler(private val sharedElementNames: List<String>) :
    SharedElementTransitionChangeHandler() {

    override fun getExitTransition(changeData: ChangeData): Transition? {
        return Fade(Fade.OUT)
    }

    override fun getSharedElementTransition(changeData: ChangeData): Transition? {
        val transition =
            TransitionSet()
                .addTransition(ChangeBounds()).addTransition(ChangeClipBounds())
                .addTransition(ChangeTransform())

        transition.pathMotion = ArcMotion()

        // The framework doesn't totally fade out the "from" shared element, so we'll hide it manually once it's safe.
        transition.addListener(object : TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                if (changeData.from != null) {
                    sharedElementNames
                        .mapNotNull { changeData.from!!.findNamedView(it) }
                        .forEach { it.visibility = View.INVISIBLE }
                }
            }

            override fun onTransitionEnd(transition: Transition) {
            }

            override fun onTransitionCancel(transition: Transition) {
            }

            override fun onTransitionPause(transition: Transition) {
            }

            override fun onTransitionResume(transition: Transition) {
            }
        })

        return transition
    }

    override fun getEnterTransition(changeData: ChangeData): Transition? {
        return Fade(Fade.IN)
    }

    override fun configureSharedElements(changeData: ChangeData) {
        sharedElementNames.forEach { addSharedElement(it) }
    }

    override fun allowTransitionOverlap(isPush: Boolean): Boolean = false

}
