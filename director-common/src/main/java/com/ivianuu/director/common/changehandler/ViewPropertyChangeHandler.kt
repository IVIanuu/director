package com.ivianuu.director.common.changehandler

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.common.ChangeData
import com.ivianuu.director.common.OnReadyOrAbortedListener

/**
 * A base [ControllerChangeHandler] that facilitates using [android.view.ViewPropertyAnimator]s to replace Controller Views
 */
abstract class ViewPropertyChangeHandler(
    duration: Long = DEFAULT_ANIMATION_DURATION,
    override var removesFromViewOnPush: Boolean = true
) : ControllerChangeHandler() {

    var duration =
        DEFAULT_ANIMATION_DURATION
        private set

    private var fromAnimator: ViewPropertyAnimator? = null
    private var toAnimator: ViewPropertyAnimator? = null

    private var onReadyOrAbortedListener: OnReadyOrAbortedListener? = null

    private var needsImmediateCompletion = false
    private var completed = false

    private var fromEnded = false
    private var toEnded = false
    private var animatorCanceled = false

    private var changeData: ChangeData? = null

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
        changeData = ChangeData(container, from, to, isPush, onChangeComplete)

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
                onReadyOrAbortedListener =
                        OnReadyOrAbortedListener(to) {
                            performAnimation(
                                container,
                                from,
                                to,
                                isPush,
                                true,
                                onChangeComplete
                            )
                        }
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

    override fun completeImmediately() {
        super.completeImmediately()
        needsImmediateCompletion = true
        if (fromAnimator != null || toAnimator != null) {
            fromAnimator?.cancel()
            toAnimator?.cancel()
        } else if (onReadyOrAbortedListener != null) {
            onReadyOrAbortedListener?.onReadyOrAborted()
        } else if (changeData != null) {
            val (container, from, _, isPush, onChangeComplete) = changeData!!
            if (from != null && (!isPush || removesFromViewOnPush)) {
                container.removeView(from)
            }

            complete(onChangeComplete)

            if (isPush && from != null) {
                resetFromView(from)
            }
        }
    }

    /**
     * Returns the animator for the [from] view
     */
    protected abstract fun getFromAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): ViewPropertyAnimator?

    /**
     * Returns the animator for the [to] view
     */
    protected abstract fun getToAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): ViewPropertyAnimator?

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

        fromAnimator = getFromAnimator(container, from, to, isPush, toAddedToContainer)?.apply {
            if (this@ViewPropertyChangeHandler.duration > 0) {
                duration = this@ViewPropertyChangeHandler.duration
            }

            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    animatorCanceled = true
                    fromEnded = true
                    onAnimationEnd(
                        container,
                        from,
                        to,
                        isPush,
                        toAddedToContainer,
                        onChangeComplete
                    )
                }

                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    fromEnded = true
                    onAnimationEnd(
                        container,
                        from,
                        to,
                        isPush,
                        toAddedToContainer,
                        onChangeComplete
                    )
                }
            })
        }

        fromAnimator?.start()

        toAnimator = getToAnimator(container, from, to, isPush, toAddedToContainer)?.apply {
            if (this@ViewPropertyChangeHandler.duration > 0) {
                duration = this@ViewPropertyChangeHandler.duration
            }

            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    animatorCanceled = true
                    toEnded = true
                    onAnimationEnd(
                        container,
                        from,
                        to,
                        isPush,
                        toAddedToContainer,
                        onChangeComplete
                    )
                }

                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    toEnded = true
                    onAnimationEnd(
                        container,
                        from,
                        to,
                        isPush,
                        toAddedToContainer,
                        onChangeComplete
                    )
                }
            })
        }

        toAnimator?.start()
    }

    private fun onAnimationEnd(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
        onChangeComplete: () -> Unit
    ) {
        if ((fromAnimator != null && !fromEnded) || (toAnimator != null && !toEnded)) {
            return
        }

        if (animatorCanceled) {
            if (from != null) {
                resetFromView(from)
            }

            if (to != null && to.parent == container) {
                container.removeView(to)
            }

            complete(onChangeComplete)
        } else {
            if (fromAnimator != null || toAnimator != null) {
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
            fromAnimator?.let { animation ->
                animation.setListener(null)
                animation.cancel()
            }

            toAnimator?.let { animation ->
                animation.setListener(null)
                animation.cancel()
            }

            onReadyOrAbortedListener = null
            changeData = null
        }
    }

    companion object {
        private const val KEY_DURATION = "ViewPropertyChangeHandler.duration"
        private const val KEY_REMOVES_FROM_ON_PUSH =
            "ViewPropertyChangeHandler.removesFromViewOnPush"
        const val DEFAULT_ANIMATION_DURATION = -1L
    }
}