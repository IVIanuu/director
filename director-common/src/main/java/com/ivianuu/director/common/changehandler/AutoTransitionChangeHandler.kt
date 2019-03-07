package com.ivianuu.director.common.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.transition.AutoTransition
import android.transition.Transition
import com.ivianuu.director.ChangeData
import com.ivianuu.director.DirectorPlugins

/**
 * A [TransitionChangeHandler] that will use an AutoTransition.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
open class AutoTransitionChangeHandler(
    duration: Long = DirectorPlugins.defaultTransitionDuration
) : TransitionChangeHandler(duration) {

    override fun getTransition(changeData: ChangeData): Transition = AutoTransition()

}