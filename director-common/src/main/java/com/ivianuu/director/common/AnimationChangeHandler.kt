package com.ivianuu.director.common

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.internal.d

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

    private var onAnimationReadyOrAbortedListener: OnAnimationReadyOrAbortedListener? = null

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
                readyToAnimate = false
                onAnimationReadyOrAbortedListener = OnAnimationReadyOrAbortedListener(
                    container,
                    from,
                    to,
                    isPush,
                    true,
                    onChangeComplete
                )

                to.viewTreeObserver.addOnPreDrawListener(onAnimationReadyOrAbortedListener)
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

        if (fromAnimation != null || toAnimation != null) {
            fromAnimation?.cancel()
            toAnimation?.cancel()
        } else if (onAnimationReadyOrAbortedListener != null) {
            onAnimationReadyOrAbortedListener?.onReadyOrAborted()
        }
    }

    override fun completeImmediately() {
        super.completeImmediately()
        needsImmediateCompletion = true
        if (fromAnimation != null || toAnimation != null) {
            fromAnimation?.cancel()
            toAnimation?.cancel()
        } else if (onAnimationReadyOrAbortedListener != null) {
            onAnimationReadyOrAbortedListener?.onReadyOrAborted()
        }
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
        d { "perform animation" }
        if (canceled) {
            d { "canceled" }
            complete(onChangeComplete)
            return
        }

        if (needsImmediateCompletion) {
            d { "needs immerdiate" }
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
                    this@AnimationChangeHandler.d { "on from animation start" }
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    this@AnimationChangeHandler.d { "on from animation end " }
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

            this@AnimationChangeHandler.d { "start from animation" }

            from?.startAnimation(this)
        }

        toAnimation = getToAnimation(container, from, to, isPush, toAddedToContainer)?.apply {
            if (this@AnimationChangeHandler.duration > 0) {
                duration = this@AnimationChangeHandler.duration
            }

            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    this@AnimationChangeHandler.d { "on to animation start" }
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    this@AnimationChangeHandler.d { "on to animation end " }
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

            this@AnimationChangeHandler.d { "start to animation" }

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
        if ((fromAnimation != null && !fromEnded) || (toAnimation != null && !toEnded)) {
            d { "wait for all animations to complete" }
            return
        }

        d { "all done" }

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
            d { "complete" }
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

        onAnimationReadyOrAbortedListener = null
    }

    private inner class OnAnimationReadyOrAbortedListener(
        val container: ViewGroup,
        val from: View?,
        val to: View?,
        val isPush: Boolean,
        val addingToView: Boolean,
        val onChangeComplete: () -> Unit
    ) : ViewTreeObserver.OnPreDrawListener {

        private var hasRun = false

        override fun onPreDraw(): Boolean {
            onReadyOrAborted()
            return true
        }

        fun onReadyOrAborted() {
            if (hasRun) return
            hasRun = true

            if (to != null) {
                val observer = to.viewTreeObserver
                if (observer.isAlive) {
                    observer.removeOnPreDrawListener(this)
                }
            }

            performAnimation(container, from, to, isPush, addingToView, onChangeComplete)
        }
    }

    companion object {
        private const val KEY_DURATION = "AnimationChangeHandler.duration"
        private const val KEY_REMOVES_FROM_ON_PUSH = "AnimationChangeHandler.removesFromViewOnPush"
        const val DEFAULT_ANIMATION_DURATION = -1L
    }
}