package com.ivianuu.director.common.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.transition.AutoTransition
import android.transition.Transition
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.DirectorPlugins
import com.ivianuu.director.RouterTransactionBuilder
import com.ivianuu.director.handler

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
        isPush: Boolean
    ): Transition = AutoTransition()

}

fun RouterTransactionBuilder.autoTransition(
    duration: Long = DirectorPlugins.defaultTransitionDuration
): RouterTransactionBuilder = apply {
    handler(AutoTransitionChangeHandler(duration))
}

fun RouterTransactionBuilder.autoTransitionPush(
    duration: Long = DirectorPlugins.defaultTransitionDuration
): RouterTransactionBuilder = apply {
    pushHandler(AutoTransitionChangeHandler(duration))
}

fun RouterTransactionBuilder.autoTransitionPop(
    duration: Long = DirectorPlugins.defaultTransitionDuration
): RouterTransactionBuilder = apply {
    popHandler(AutoTransitionChangeHandler(duration))
}