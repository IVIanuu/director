package com.ivianuu.director.internal

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup

internal class ControllerAttachHandler(
    private val listener: (attached: Boolean, fromHost: Boolean, viewDetachAfterStop: Boolean) -> Unit
) : OnAttachStateChangeListener {

    private var rootAttached = false
    private var childrenAttached = false
    private var hostStarted = false

    private var awaitingHostStart = false

    private var reportedState = ReportedState.VIEW_DETACHED

    private var childOnAttachStateChangeListener: OnAttachStateChangeListener? = null

    private var container: ViewGroup? = null
    private var view: View? = null

    override fun onViewAttachedToWindow(v: View) {
        // explicitly check the container
        // we could get attached to another container while transitioning
        if (!rootAttached && v.parent == container) {
            rootAttached = true

            listenForDeepestChildAttach(v) {
                childrenAttached = true
                notifyChange(false)
            }
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        if (rootAttached) {
            rootAttached = false
            if (childrenAttached) {
                childrenAttached = false
                notifyChange(false)
            }
        }
    }

    fun takeView(container: ViewGroup, view: View) {
        this.container = container
        this.view = view
        view.addOnAttachStateChangeListener(this)
    }

    fun dropView() {
        rootAttached = false
        childrenAttached = false

        container = null

        view?.let { view ->
            view.removeOnAttachStateChangeListener(this)
            if (childOnAttachStateChangeListener != null && view is ViewGroup) {
                findDeepestChild(view).removeOnAttachStateChangeListener(
                    childOnAttachStateChangeListener
                )
            }
        }

        view = null
    }

    fun hostStarted() {
        if (!hostStarted) {
            hostStarted = true
            notifyChange(true)
        }
    }

    fun hostStopped() {
        if (hostStarted) {
            hostStarted = false
            notifyChange(true)
        }
    }

    private fun notifyChange(fromHost: Boolean) {
        val viewAttached = rootAttached && childrenAttached

        if (viewAttached && reportedState != ReportedState.ATTACHED) {
            reportedState = ReportedState.ATTACHED
            listener(true, fromHost, false)
            return
        }

        val wasDetachedForHost = reportedState == ReportedState.HOST_STOPPED

        reportedState = if (fromHost) {
            ReportedState.HOST_STOPPED
        } else {
            ReportedState.VIEW_DETACHED
        }

        val detachAfterStop = wasDetachedForHost && !fromHost

        listener(false, fromHost, detachAfterStop)
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
        ATTACHED, HOST_STOPPED, VIEW_DETACHED
    }
}
