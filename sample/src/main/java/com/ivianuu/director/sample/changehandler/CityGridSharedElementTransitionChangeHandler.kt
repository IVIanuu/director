package com.ivianuu.director.sample.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
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
import java.util.*

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class CityGridSharedElementTransitionChangeHandler(
    names: List<String> = emptyList()
) : SharedElementTransitionChangeHandler() {

    private val names = mutableListOf<String>()

    init {
        this.names.addAll(names)
    }

    override fun saveToBundle(bundle: Bundle) {
        bundle.putStringArrayList(KEY_WAIT_FOR_TRANSITION_NAMES, ArrayList(names))
    }

    override fun restoreFromBundle(bundle: Bundle) {
        val savedNames = bundle.getStringArrayList(KEY_WAIT_FOR_TRANSITION_NAMES)
        if (savedNames != null) {
            names.addAll(savedNames)
        }
    }

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
        names.forEach {
            addSharedElement(it)
            waitOnSharedElementNamed(it)
        }
    }

    companion object {
        private const val KEY_WAIT_FOR_TRANSITION_NAMES =
            "CityGridSharedElementTransitionChangeHandler.names"
    }
}
