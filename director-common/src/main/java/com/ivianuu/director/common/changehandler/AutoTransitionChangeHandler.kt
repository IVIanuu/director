package com.ivianuu.director.common.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.transition.AutoTransition
import android.transition.Transition
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.DirectorPlugins

/**
 * A [TransitionChangeHandler] that will use an AutoTransition.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
open class AutoTransitionChangeHandler(
    duration: Long = DirectorPlugins.defaultTransitionDuration
) : TransitionChangeHandler(duration) {

    override fun getTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        toIndex: Int,
        isPush: Boolean
    ): Transition = AutoTransition()

}