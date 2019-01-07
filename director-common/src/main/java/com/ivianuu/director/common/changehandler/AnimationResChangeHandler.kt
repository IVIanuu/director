package com.ivianuu.director.common.changehandler

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.ivianuu.director.ControllerChangeHandler

/**
 * A base [ControllerChangeHandler] that uses [android.view.animation.Animation]
 * from xml files s to replace Controller Views
 */
open class AnimationResChangeHandler :
    AnimationChangeHandler {

    private var pushToAnimationRes = 0
    private var pushFromAnimationRes = 0
    private var popFromAnimationRes = 0
    private var popToAnimationRes = 0

    constructor() : super()

    constructor(
        pushToAnimationRes: Int = 0,
        pushFromAnimationRes: Int = 0,
        popFromAnimationRes: Int = 0,
        popToAnimationRes: Int = 0,
        removesFromViewOnPush: Boolean = true
    ) : super(removesFromViewOnPush = removesFromViewOnPush) {
        this.pushToAnimationRes = pushToAnimationRes
        this.pushFromAnimationRes = pushFromAnimationRes
        this.popFromAnimationRes = popFromAnimationRes
        this.popToAnimationRes = popToAnimationRes
    }

    override fun getFromAnimation(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animation? {
        return if (isPush && pushFromAnimationRes != 0) {
            AnimationUtils.loadAnimation(container.context, pushFromAnimationRes)
        } else if (!isPush && popFromAnimationRes != 0) {
            AnimationUtils.loadAnimation(container.context, popFromAnimationRes)
        } else {
            null
        }
    }

    override fun getToAnimation(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animation? {
        return if (isPush && pushToAnimationRes != 0) {
            AnimationUtils.loadAnimation(container.context, pushToAnimationRes)
        } else if (!isPush && popToAnimationRes != 0) {
            AnimationUtils.loadAnimation(container.context, popToAnimationRes)
        } else {
            null
        }
    }

    override fun resetFromView(from: View) {
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putInt(KEY_PUSH_TO_ANIMATION_RES, pushToAnimationRes)
        bundle.putInt(KEY_PUSH_FROM_ANIMATION_RES, pushFromAnimationRes)
        bundle.putInt(KEY_POP_FROM_ANIMATION_RES, popFromAnimationRes)
        bundle.putInt(KEY_POP_TO_ANIMATION_RES, popToAnimationRes)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        pushToAnimationRes = bundle.getInt(KEY_PUSH_TO_ANIMATION_RES)
        pushFromAnimationRes = bundle.getInt(KEY_PUSH_FROM_ANIMATION_RES)
        popFromAnimationRes = bundle.getInt(KEY_POP_FROM_ANIMATION_RES)
        popToAnimationRes = bundle.getInt(KEY_POP_TO_ANIMATION_RES)
    }

    private companion object {
        private const val KEY_PUSH_TO_ANIMATION_RES = "AnimationResChangeHandler.pushToAnimationRes"
        private const val KEY_PUSH_FROM_ANIMATION_RES =
            "AnimationResChangeHandler.pushFromAnimationRes"
        private const val KEY_POP_FROM_ANIMATION_RES =
            "AnimationResChangeHandler.popFromAnimationRes"
        private const val KEY_POP_TO_ANIMATION_RES = "AnimationResChangeHandler.popToAnimationRes"
    }
}