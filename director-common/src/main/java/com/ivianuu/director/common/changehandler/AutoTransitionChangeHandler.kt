package com.ivianuu.director.common.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.transition.AutoTransition
import android.transition.Transition
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.RouterTransactionBuilder
import com.ivianuu.director.handler

/**
 * A [TransitionChangeHandler] that will use an AutoTransition.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
open class AutoTransitionChangeHandler : TransitionChangeHandler() {

    override fun getTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition = AutoTransition()

}

fun RouterTransactionBuilder.autoTransition(): RouterTransactionBuilder = apply {
    handler(AutoTransitionChangeHandler())
}

fun RouterTransactionBuilder.autoTransitionPush(): RouterTransactionBuilder = apply {
    pushHandler(AutoTransitionChangeHandler())
}

fun RouterTransactionBuilder.autoTransitionPop(): RouterTransactionBuilder = apply {
    popHandler(AutoTransitionChangeHandler())
}