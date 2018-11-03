package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup

import com.ivianuu.director.ControllerChangeHandler

internal class NoOpControllerChangeHandler : ControllerChangeHandler() {

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
        onChangeComplete()
    }

    override fun copy() = NoOpControllerChangeHandler()
}
