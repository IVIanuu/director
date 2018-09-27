package com.ivianuu.director.sample.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat

import com.ivianuu.director.common.TransitionChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.transitions.FabTransform
import com.ivianuu.director.sample.util.AnimUtils

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class FabToDialogTransitionChangeHandler : TransitionChangeHandler() {

    private var fab: View? = null
    private var dialogBackground: View? = null
    private var fabParent: ViewGroup? = null

    override val removesFromViewOnPush: Boolean
        get() = false

    override fun getTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition {
        val backgroundFade = Fade()
        backgroundFade.addTarget(R.id.dialog_background)

        val fabTransform = FabTransform(
            ContextCompat.getColor(container.context, R.color.colorAccent),
            R.drawable.ic_github_face
        )

        val set = TransitionSet()
        set.addTransition(backgroundFade)
        set.addTransition(fabTransform)

        return set
    }

    override fun prepareForTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition,
        isPush: Boolean,
        onTransitionPrepared: () -> Unit
    ) {
        fab = if (isPush) from!!.findViewById(R.id.fab) else to!!.findViewById(R.id.fab)
        fabParent = fab!!.parent as ViewGroup

        if (!isPush) {
            /*
             * Before we transition back we want to remove the fab
             * in order to add it again for the TransitionManager to be able to detect the change
             */
            fabParent!!.removeView(fab)
            fab!!.visibility = View.VISIBLE

            /*
             * Before we transition back we need to move the dialog's background to the new view
             * so its fade won't take place over the fab transition
             */
            dialogBackground = from!!.findViewById(R.id.dialog_background)
            (dialogBackground!!.parent as ViewGroup).removeView(dialogBackground)
            fabParent!!.addView(dialogBackground)
        }

        onTransitionPrepared()
    }

    override fun executePropertyChanges(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition?,
        isPush: Boolean
    ) {
        if (isPush) {
            fabParent!!.removeView(fab)
            container.addView(to)

            fun onTransitionComplete() {
                fab!!.visibility = View.GONE
                fabParent!!.addView(fab)
                fab = null
                fabParent = null
            }

            /*
             * After the transition is finished we have to add the fab back to the original container.
             * Because otherwise we will be lost when trying to transition back.
             * Set it to invisible because we don't want it to jump back after the transition
             */
            val endListener = object : AnimUtils.TransitionEndListener() {
                override fun onTransitionCompleted(transition: Transition) {
                    onTransitionComplete()
                }
            }
            if (transition != null) {
                transition.addListener(endListener)
            } else {
                onTransitionComplete()
            }
        } else {
            dialogBackground!!.visibility = View.INVISIBLE
            fabParent!!.addView(fab)
            container.removeView(from)

            fun onTransitionComplete() {
                fabParent!!.removeView(dialogBackground)
                dialogBackground = null
            }

            val endListener = object : AnimUtils.TransitionEndListener() {
                override fun onTransitionCompleted(transition: Transition) {
                    onTransitionComplete()
                }
            }
            if (transition != null) {
                transition.addListener(endListener)
            } else {
                onTransitionComplete()
            }
        }
    }
}
