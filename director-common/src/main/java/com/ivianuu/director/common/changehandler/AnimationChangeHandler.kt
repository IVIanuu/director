package com.ivianuu.director.common.changehandler

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.common.OnReadyOrAbortedListener

/**
 * A base [ControllerChangeHandler] that facilitates using [android.view.animation.Animation]s to replace Controller Views
 */
abstract class AnimationChangeHandler(
    duration: Long = DEFAULT_ANIMATION_DURATION,
    override var removesFromViewOnPush: Boolean = true
) : ControllerChangeHandler() {

    var duration =
        DEFAULT_ANIMATION_DURATION
        private set

    private var fromAnimation: Animation? = null
    private var toAnimation: Animation? = null

    private var onReadyOrAbortedListener: OnReadyOrAbortedListener? = null

    private var canceled = false
    private var needsImmediateCompletion = false
    private var completed = false

    private var fromEnded = false
    private var toEnded = false

    init {
        this.duration = duration
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
        var readyToAnimate = true
        val addingToView = to != null && to.parent == null

        if (addingToView) {
            if (isPush || from == null) {
                container.addView(to)
            } else if (to?.parent == null) {
                container.addView(to, container.indexOfChild(from))
            }

            if (to!!.width <= 0 && to.height <= 0) {
                onReadyOrAbortedListener =
                        OnReadyOrAbortedListener(to) {
                            performAnimation(container, from, to, isPush, true, onChangeComplete)
                        }
                readyToAnimate = false
            }
        }

        if (readyToAnimate) {
            performAnimation(container, from, to, isPush, addingToView, onChangeComplete)
        }
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putLong(KEY_DURATION, duration)
        bundle.putBoolean(KEY_REMOVES_FROM_ON_PUSH, removesFromViewOnPush)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        duration = bundle.getLong(KEY_DURATION)
        removesFromViewOnPush = bundle.getBoolean(KEY_REMOVES_FROM_ON_PUSH)
    }

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)
        canceled = true
        fromAnimation?.cancel()
        toAnimation?.cancel()
        onReadyOrAbortedListener?.onReadyOrAborted()
    }

    override fun completeImmediately() {
        super.completeImmediately()
        needsImmediateCompletion = true
        fromAnimation?.cancel()
        toAnimation?.cancel()
        onReadyOrAbortedListener?.onReadyOrAborted()
    }

    /**
     * Returns the animation for the [from] view
     */
    protected abstract fun getFromAnimation(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animation?

    /**
     * Returns the animation for the [to] view
     */
    protected abstract fun getToAnimation(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animation?

    /**
     * Will be called after the animation is complete to reset the View that was removed to its pre-animation state.
     */
    protected abstract fun resetFromView(from: View)

    private fun performAnimation(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
        onChangeComplete: () -> Unit
    ) {
        if (canceled) {
            complete(onChangeComplete)
            return
        }

        if (needsImmediateCompletion) {
            if (from != null && (!isPush || removesFromViewOnPush)) {
                container.removeView(from)
            }
            complete(onChangeComplete)
            if (isPush && from != null) {
                resetFromView(from)
            }
            return
        }

        fromAnimation = getFromAnimation(container, from, to, isPush, toAddedToContainer)?.apply {
            if (this@AnimationChangeHandler.duration > 0) {
                duration = this@AnimationChangeHandler.duration
            }

            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    fromEnded = true
                    container.post {
                        onAnimationEnd(
                            container,
                            from,
                            to,
                            isPush,
                            toAddedToContainer,
                            onChangeComplete
                        )
                    }
                }
            })

            from?.startAnimation(this)
        }

        toAnimation = getToAnimation(container, from, to, isPush, toAddedToContainer)?.apply {
            if (this@AnimationChangeHandler.duration > 0) {
                duration = this@AnimationChangeHandler.duration
            }

            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    toEnded = true
                    container.post {
                        onAnimationEnd(
                            container,
                            from,
                            to,
                            isPush,
                            toAddedToContainer,
                            onChangeComplete
                        )
                    }
                }
            })

            to?.startAnimation(this)
        }
    }

    private fun onAnimationEnd(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
        onChangeComplete: () -> Unit
    ) {
        if ((fromAnimation != null && !fromEnded) || (toAnimation != null && !toEnded)) return

        if (canceled) {
            if (from != null) {
                resetFromView(from)
            }

            if (to != null && to.parent == container) {
                container.removeView(to)
            }

            complete(onChangeComplete)
        } else {
            if (!canceled && (fromAnimation != null || toAnimation != null)) {
                if (from != null && (!isPush || removesFromViewOnPush)) {
                    container.removeView(from)
                }

                complete(onChangeComplete)

                if (isPush && from != null) {
                    resetFromView(from)
                }
            }
        }
    }

    private fun complete(onChangeComplete: () -> Unit) {
        if (!completed) {
            completed = true
            onChangeComplete()
        }

        fromAnimation?.let { animation ->
            animation.setAnimationListener(null)
            animation.cancel()
        }

        toAnimation?.let { animation ->
            animation.setAnimationListener(null)
            animation.cancel()
        }

        onReadyOrAbortedListener = null
    }

    companion object {
        private const val KEY_DURATION = "AnimationChangeHandler.duration"
        private const val KEY_REMOVES_FROM_ON_PUSH = "AnimationChangeHandler.removesFromViewOnPush"
        const val DEFAULT_ANIMATION_DURATION = -1L
    }
}