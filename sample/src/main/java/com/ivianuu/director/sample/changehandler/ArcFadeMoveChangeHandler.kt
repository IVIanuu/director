package com.ivianuu.director.sample.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.transition.ArcMotion
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.common.SharedElementTransitionChangeHandler
import com.ivianuu.director.common.TransitionUtils
import java.util.*

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ArcFadeMoveChangeHandler : SharedElementTransitionChangeHandler {

    private val sharedElementNames = mutableListOf<String>()

    constructor()

    constructor(vararg sharedElementNames: String) {
        this.sharedElementNames.addAll(sharedElementNames)
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putStringArrayList(KEY_SHARED_ELEMENT_NAMES, ArrayList(sharedElementNames))
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)

        sharedElementNames.addAll(bundle.getStringArrayList(KEY_SHARED_ELEMENT_NAMES)!!)
    }

    override fun getExitTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? {
        return Fade(Fade.OUT)
    }

    override fun getSharedElementTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? {
        val transition =
            TransitionSet()
                .addTransition(ChangeBounds()).addTransition(ChangeClipBounds())
                .addTransition(ChangeTransform())

        transition.pathMotion = ArcMotion()

        // The framework doesn't totally fade out the "from" shared element, so we'll hide it manually once it's safe.
        transition.addListener(object : TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                if (from != null) {
                    sharedElementNames
                        .mapNotNull { TransitionUtils.findNamedView(from, it) }
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

    override fun getEnterTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? {
        return Fade(Fade.IN)
    }

    override fun configureSharedElements(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ) {
        sharedElementNames.forEach { addSharedElement(it) }
    }

    override fun allowTransitionOverlap(isPush: Boolean) = false

    companion object {
        private const val KEY_SHARED_ELEMENT_NAMES = "ArcFadeMoveChangeHandler.sharedElementNames"
    }
}
