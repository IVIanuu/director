package com.ivianuu.director.common

import android.view.View
import android.view.ViewTreeObserver

class OnReadyOrAbortedListener(
    val view: View,
    val performAnimation: () -> Unit
) : ViewTreeObserver.OnPreDrawListener {

    private var hasRun = false

    init {
        view.viewTreeObserver.addOnPreDrawListener(this)
    }

    override fun onPreDraw(): Boolean {
        onReadyOrAborted()
        return true
    }

    fun onReadyOrAborted() {
        if (hasRun) return
        hasRun = true

        val observer = view.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnPreDrawListener(this)
        }

        performAnimation()
    }
}