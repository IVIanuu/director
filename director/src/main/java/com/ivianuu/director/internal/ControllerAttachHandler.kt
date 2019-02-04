package com.ivianuu.director.internal

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup

internal class ControllerAttachHandler(private val listener: Listener) :
    OnAttachStateChangeListener {

    private var rootAttached = false
    private var childrenAttached = false
    private var hostReady = false

    private var reportedState = ReportedState.VIEW_DETACHED

    private var childOnAttachStateChangeListener: OnAttachStateChangeListener? = null

    override fun onViewAttachedToWindow(v: View) {
        if (!rootAttached) {
            rootAttached = true

            listenForDeepestChildAttach(v) {
                childrenAttached = true
                reportAttached()
            }
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        rootAttached = false
        if (childrenAttached) {
            childrenAttached = false
            reportDetached(false)
        }
    }

    fun listenForAttach(view: View) {
        view.addOnAttachStateChangeListener(this)
    }

    fun unregisterAttachListener(view: View) {
        view.removeOnAttachStateChangeListener(this)

        if (childOnAttachStateChangeListener != null && view is ViewGroup) {
            findDeepestChild(view).removeOnAttachStateChangeListener(
                childOnAttachStateChangeListener
            )
        }
    }

    fun hostStarted() {
        hostReady = true
        reportAttached()
    }

    fun hostStopped() {
        hostReady = false
        reportDetached(true)
    }

    private fun reportAttached() {
        if (rootAttached && childrenAttached && hostReady && reportedState != ReportedState.ATTACHED) {
            reportedState = ReportedState.ATTACHED
            listener.onAttached()
        }
    }

    private fun reportDetached(detachedForActivity: Boolean) {
        val wasDetachedForActivity = reportedState == ReportedState.ACTIVITY_STOPPED

        reportedState = if (detachedForActivity) {
            ReportedState.ACTIVITY_STOPPED
        } else {
            ReportedState.VIEW_DETACHED
        }

        if (wasDetachedForActivity && !detachedForActivity) {
            listener.onViewDetachAfterStop()
        } else {
            listener.onDetached(detachedForActivity)
        }
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

    private enum class ReportedState {
        VIEW_DETACHED,
        ACTIVITY_STOPPED,
        ATTACHED
    }

    interface Listener {
        fun onAttached()
        fun onDetached(fromActivityStop: Boolean)
        fun onViewDetachAfterStop()
    }
}
