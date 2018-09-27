package com.ivianuu.director

/**
 * All possible types of [Controller] changes to be used in [ControllerChangeHandler]s
 */
enum class ControllerChangeType(val isPush: Boolean, val isEnter: Boolean) {
    /** The Controller is being pushed to the host container  */
    PUSH_ENTER(true, true),

    /** The Controller is being pushed to the backstack as another Controller is pushed to the host container  */
    PUSH_EXIT(true, false),

    /** The Controller is being popped from the backstack and placed in the host container as another Controller is popped  */
    POP_ENTER(false, true),

    /** The Controller is being popped from the host container  */
    POP_EXIT(false, false)
}
