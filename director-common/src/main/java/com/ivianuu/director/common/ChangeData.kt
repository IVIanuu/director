package com.ivianuu.director.common

import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.ControllerChangeHandler

/**
 * Simple helper class to wrap all params of a [ControllerChangeHandler.performChange] call
 */
data class ChangeData(
    val container: ViewGroup,
    val from: View?,
    val to: View?,
    val toIndex: Int,
    val isPush: Boolean,
    val onChangeComplete: () -> Unit
)