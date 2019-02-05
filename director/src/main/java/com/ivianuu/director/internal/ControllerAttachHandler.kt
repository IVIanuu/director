package com.ivianuu.director.internal

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup

internal class ControllerAttachHandler(
    private val hasParent: Boolean,
    private val listener: (reason: ChangeReason, viewAttached: Boolean, parentAttached: Boolean, hostStarted: Boolean) -> Unit
) : OnAttachStateChangeListener {

    private var rootAttached = false
    private var childrenAttached = false
    private var hostStarted = false
    private var parentAttached = false

    private var lastReason = ChangeReason.VIEW
    private var wasViewAttached = false
    private var wasHostStarted = false
    private var wasParentAttached = false

    private var childOnAttachStateChangeListener: OnAttachStateChangeListener? = null

    override fun onViewAttachedToWindow(v: View) {
        if (!rootAttached) {
            rootAttached = true

            listenForDeepestChildAttach(v) {
                childrenAttached = true
                notifyChange(ChangeReason.VIEW)
            }
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        if (rootAttached) {
            rootAttached = false
            if (childrenAttached) {
                childrenAttached = false
                notifyChange(ChangeReason.VIEW)
            }
        }
    }

    fun takeView(view: View) {
        view.addOnAttachStateChangeListener(this)
    }

    fun dropView(view: View) {
        rootAttached = false
        childrenAttached = false

        view.removeOnAttachStateChangeListener(this)

        if (childOnAttachStateChangeListener != null && view is ViewGroup) {
            findDeepestChild(view).removeOnAttachStateChangeListener(
                childOnAttachStateChangeListener
            )
        }
    }

    fun hostStarted() {
        if (!hostStarted) {
            hostStarted = true
            notifyChange(ChangeReason.HOST)
        }
    }

    fun hostStopped() {
        if (hostStarted) {
            hostStarted = false
            notifyChange(ChangeReason.HOST)
        }
    }

    fun parentAttached() {
        if (!parentAttached) {
            parentAttached = true
            notifyChange(ChangeReason.PARENT)
        }
    }

    fun parentDetached() {
        if (parentAttached) {
            parentAttached = false
            notifyChange(ChangeReason.PARENT)
        }
    }

    private fun notifyChange(reason: ChangeReason) {
        listener(
            reason, rootAttached && childrenAttached,
            !hasParent || parentAttached, hostStarted
        )
    }

    private fun listenForDeepestChildAttach(view: View, onAttach: () -> Unit) {
        if (view !is ViewGroup) {
            onAttach()
            return
        }

        if (view.childCount == 0) {
            onAttach()
            return
        }

        childOnAttachStateChangeListener = object : OnAttachStateChangeListener {

            private var attached = false

            override fun onViewAttachedToWindow(v: View) {
                if (!attached) {
                    attached = true
                    onAttach()
                    v.removeOnAttachStateChangeListener(this)
                    childOnAttachStateChangeListener = null
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
            }
        }

        findDeepestChild(view).addOnAttachStateChangeListener(childOnAttachStateChangeListener)
    }

    private fun findDeepestChild(viewGroup: ViewGroup): View {
        if (viewGroup.childCount == 0) return viewGroup

        val lastChild = viewGroup.getChildAt(viewGroup.childCount - 1)
        return if (lastChild is ViewGroup) {
            findDeepestChild(lastChild)
        } else {
            lastChild
        }
    }

    enum class ChangeReason {
        VIEW, PARENT, HOST
    }
}
