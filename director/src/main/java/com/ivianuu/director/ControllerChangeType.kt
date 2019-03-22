package com.ivianuu.director

/**
 * All possible types of [Controller] changes to be used in [ChangeHandler]s
 */
enum class ControllerChangeType(val isPush: Boolean, val isEnter: Boolean) {
    PUSH_ENTER(true, true),
    PUSH_EXIT(true, false),
    POP_ENTER(false, true),
    POP_EXIT(false, false)
}