package com.ivianuu.director

/**
 * Fluent version of push change handler
 */
fun RouterTransaction.pushChangeHandler(changeHandler: ControllerChangeHandler?) = apply {
    pushChangeHandler = changeHandler
}

/**
 * Fluent version of pop change handler
 */
fun RouterTransaction.popChangeHandler(changeHandler: ControllerChangeHandler?) = apply {
    popChangeHandler = changeHandler
}

/**
 * Fluent version of tag
 */
fun RouterTransaction.tag(tag: String?) = apply {
    this.tag = tag
}