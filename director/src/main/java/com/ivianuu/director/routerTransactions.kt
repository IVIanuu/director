package com.ivianuu.director

/**
 * Returns a new [RouterTransaction] with the [controller]
 */
fun RouterTransaction(controller: Controller) = RouterTransaction(controller, Unit)

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