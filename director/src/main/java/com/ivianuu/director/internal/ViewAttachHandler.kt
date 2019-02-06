package com.ivianuu.director.internal

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup

internal class ViewAttachHandler(
    private val listener: (attached: Boolean) -> Unit
) : OnAttachStateChangeListener {

    private var rootAttached = false
    private var childrenAttached = false

    private var reportedState = false

    private var childOnAttachStateChangeListener: OnAttachStateChangeListener? = null

    override fun onViewAttachedToWindow(v: View) {
        // explicitly check the container
        // we could get attached to another container while transitioning
        if (!rootAttached) {
            rootAttached = true

            listenForDeepestChildAttach(v) {
                childrenAttached = true
                notifyChange()
            }
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        if (rootAttached) {
            rootAttached = false
            if (childrenAttached) {
                childrenAttached = false
                notifyChange()
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

    private fun notifyChange() {
        val attached = rootAttached && childrenAttached

        if (attached != reportedState) {
            reportedState = attached
            listener(attached)
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

}
