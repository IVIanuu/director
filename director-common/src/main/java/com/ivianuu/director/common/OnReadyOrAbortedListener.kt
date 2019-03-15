package com.ivianuu.director.common

import android.view.View
import android.view.ViewTreeObserver

class OnReadyOrAbortedListener(
    val view: View,
    val action: () -> Unit
) : ViewTreeObserver.OnPreDrawListener {

    private var finished = false

    init {
        view.viewTreeObserver.addOnPreDrawListener(this)
    }

    override fun onPreDraw(): Boolean {
        onReadyOrAborted()
        return true
    }

    fun onReadyOrAborted() {
        if (finished) return
        finished = true

        val observer = view.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnPreDrawListener(this)
        }

        action()
    }
}