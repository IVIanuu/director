package com.ivianuu.director.common

import android.view.View
import android.view.ViewGroup

/**
 * Simple helper class to wrap all params of a [com.ivianuu.director.ControllerChangeHandler.performChange] call
 */
data class ChangeData(
    val container: ViewGroup,
    val from: View?,
    val to: View?,
    val isPush: Boolean,
    val onChangeComplete: () -> Unit
)