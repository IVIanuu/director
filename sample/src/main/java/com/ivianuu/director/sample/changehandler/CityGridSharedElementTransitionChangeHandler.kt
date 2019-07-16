package com.ivianuu.director.sample.changehandler

import android.annotation.TargetApi
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeTransform
import android.transition.Explode
import android.transition.Slide
import android.transition.Transition
import android.transition.TransitionSet
import android.view.Gravity
import com.ivianuu.director.ChangeData
import com.ivianuu.director.common.changehandler.SharedElementTransitionChangeHandler

@TargetApi(21)
class CityGridSharedElementTransitionChangeHandler(
    private val sharedElementNames: List<String>
) : SharedElementTransitionChangeHandler() {

    override fun getExitTransition(changeData: ChangeData): Transition? {
        return if (changeData.isPush) {
            Explode()
        } else {
            Slide(Gravity.BOTTOM)
        }
    }

    override fun getSharedElementTransition(changeData: ChangeData): Transition? {
        return TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeClipBounds())
            .addTransition(ChangeTransform())
    }

    override fun getEnterTransition(changeData: ChangeData): Transition? {
        return if (changeData.isPush) {
            Slide(Gravity.BOTTOM)
        } else {
            Explode()
        }
    }

    override fun configureSharedElements(changeData: ChangeData) {
        sharedElementNames.forEach {
            addSharedElement(it)
            waitOnSharedElementNamed(it)
        }
    }

    override fun copy() = CityGridSharedElementTransitionChangeHandler(sharedElementNames)

}
