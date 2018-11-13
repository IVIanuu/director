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
import android.view.View
import android.view.ViewGroup
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

    override fun getExitTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? = if (isPush) {
        Explode()
    } else {
        Slide(Gravity.BOTTOM)
    }

    override fun getSharedElementTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? = TransitionSet()
        .addTransition(ChangeBounds())
        .addTransition(ChangeClipBounds())
        .addTransition(ChangeTransform())

    override fun getEnterTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? = if (isPush) {
        Slide(Gravity.BOTTOM)
    } else {
        Explode()
    }.apply {
        names.forEach { excludeTarget(it, true) }
    }

    override fun configureSharedElements(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ) {
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
