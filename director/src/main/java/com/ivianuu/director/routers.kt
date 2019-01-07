/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director

import android.os.Bundle

internal val Router.hasContainer: Boolean get() = container != null

/**
 * The current size of the backstack
 */
val Router.backstackSize: Int get() = backstack.size

/**
 * Whether or not this router has root [Controller]
 */
val Router.hasRootController: Boolean get() = backstackSize > 0

/**
 * The container id which will be used by this router
 */
val Router.containerId: Int
    get() = container?.id ?: 0

/**
 * Fluent version of pops last view
 */
fun Router.popsLastView(popsLastView: Boolean): Router = apply { this.popsLastView = popsLastView }

/**
 * Saves the instance state of [controller] which can later be used in
 * [Controller.setInitialSavedState]
 */
fun Router.saveControllerInstanceState(controller: Controller): Bundle {
    if (backstack.none { it.controller == controller }) {
        throw IllegalArgumentException("controller is not attached to the router")
    }

    return controller.saveInstanceState()
}

/**
 * Returns the hosted Controller with the given instance id or `null` if no such
 * Controller exists in this Router.
 */
fun Router.findControllerByInstanceId(instanceId: String): Controller? {
    return backstack
        .mapNotNull { it.controller.findController(instanceId) }
        .firstOrNull()
}

/**
 * Returns the hosted Controller that was pushed with the given tag or `null` if no
 * such Controller exists in this Router.
 */
fun Router.findControllerByTag(tag: String): Controller? =
    backstack.firstOrNull { it.tag == tag }?.controller

/**
 * This should be called by the host Activity when its onBackPressed method is called. The call will be forwarded
 * to its top [Controller]. If that controller doesn't handle it, then it will be popped.
 */
fun Router.handleBack(): Boolean {
    val currentTransaction = backstack.lastOrNull()

    return if (currentTransaction != null) {
        if (currentTransaction.controller.handleBack()) {
            true
        } else if (backstackSize > 0 && (popsLastView || backstackSize > 1)) {
            popCurrentController()
            true
        } else {
            false
        }
    } else {
        false
    }
}

/**
 * Pops the top [Controller] from the backstack
 */
fun Router.popCurrentController(changeHandler: ControllerChangeHandler? = null) {
    val transaction = backstack.lastOrNull()
        ?: throw IllegalStateException("Trying to pop the current controller when there are none on the backstack.")
    popController(transaction.controller, changeHandler)
}

/**
 * Pops the passed [Controller] from the backstack
 */
fun Router.popController(
    controller: Controller,
    changeHandler: ControllerChangeHandler? = null
) {
    val oldBackstack = backstack
    val newBackstack = oldBackstack.toMutableList()
    newBackstack.removeAll { it.controller == controller }

    val topTransaction = oldBackstack.lastOrNull()
    val poppingTopController = topTransaction?.controller == controller

    val changeHandler = changeHandler ?: if (poppingTopController) {
        topTransaction!!.popChangeHandler
    } else {
        null
    }

    setBackstack(newBackstack, changeHandler)
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.pushController(
    transaction: RouterTransaction,
    changeHandler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(transaction)
    if (changeHandler != null) {
        setBackstack(newBackstack, changeHandler)
    } else {
        setBackstack(newBackstack)
    }
}

/**
 * Replaces this Router's top [Controller] with the [Controller] of the [transaction]
 */
fun Router.replaceTopController(
    transaction: RouterTransaction,
    changeHandler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    val from = newBackstack.lastOrNull()
    if (from != null) {
        newBackstack.removeAt(newBackstack.lastIndex)
    }
    newBackstack.add(transaction)

    if (changeHandler != null) {
        setBackstack(newBackstack, changeHandler)
    } else {
        setBackstack(newBackstack)
    }
}


/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(changeHandler: ControllerChangeHandler? = null) {
    backstack.firstOrNull()
        ?.let { popToTransaction(it, changeHandler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, changeHandler: ControllerChangeHandler? = null) {
    backstack.firstOrNull { it.tag == tag }
        ?.let { popToTransaction(it, changeHandler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popToController(
    controller: Controller,
    changeHandler: ControllerChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { popToTransaction(it, changeHandler) }
}

/***
 * Pops all [Controller]s until the [transaction] is at the top
 */
fun Router.popToTransaction(
    transaction: RouterTransaction,
    changeHandler: ControllerChangeHandler? = null
) {
    if (backstack.isNotEmpty()) {
        val topTransaction = backstack.lastOrNull()
        val newBackstack = backstack.dropLastWhile { it != transaction }
        setBackstack(newBackstack, changeHandler ?: topTransaction?.popChangeHandler)
    }
}

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun Router.setRoot(transaction: RouterTransaction, changeHandler: ControllerChangeHandler? = null) {
    setBackstack(listOf(transaction), changeHandler ?: transaction.pushChangeHandler)
}