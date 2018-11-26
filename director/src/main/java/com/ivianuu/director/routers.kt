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
import com.ivianuu.director.internal.requireMainThread

/**
 * The current size of the backstack
 */
val Router.backstackSize get() = backstack.size

/**
 * Whether or not this router has root [Controller]
 */
val Router.hasRootController get() = backstackSize > 0

/**
 * The container id which will be used by this router
 */
val Router.containerId: Int
    get() = container?.id ?: 0

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
fun Router.findControllerByInstanceId(instanceId: String) = backstack
    .mapNotNull { it.controller.findController(instanceId) }
    .firstOrNull()

/**
 * Returns the hosted Controller that was pushed with the given tag or `null` if no
 * such Controller exists in this Router.
 */
fun Router.findControllerByTag(tag: String) =
    backstack.firstOrNull { it.tag == tag }?.controller

/**
 * This should be called by the host Activity when its onBackPressed method is called. The call will be forwarded
 * to its top [Controller]. If that controller doesn't handle it, then it will be popped.
 */
fun Router.handleBack(): Boolean {
    requireMainThread()

    val currentTransaction = backstack.lastOrNull()
    if (currentTransaction != null) {
        if (currentTransaction.controller.handleBack()) {
            return true
        } else if (popCurrentController()) {
            return true
        }
    }

    return false
}

/**
 * Pops the top [Controller] from the backstack
 */
fun Router.popCurrentController(changeHandler: ControllerChangeHandler? = null): Boolean {
    val transaction = backstack.lastOrNull()
        ?: throw IllegalStateException("Trying to pop the current controller when there are none on the backstack.")
    return popController(transaction.controller, changeHandler)
}

/**
 * Pops the passed [Controller] from the backstack
 */
fun Router.popController(
    controller: Controller,
    changeHandler: ControllerChangeHandler? = null
): Boolean {
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

    return if (popsLastView) {
        oldBackstack.isNotEmpty()
    } else {
        newBackstack.isNotEmpty()
    }
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.pushController(transaction: RouterTransaction) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(transaction)
    setBackstack(newBackstack)
}

/**
 * Replaces this Router's top [Controller] with a new [Controller]
 */
fun Router.replaceTopController(transaction: RouterTransaction) {
    val newBackstack = backstack.toMutableList()
    val from = newBackstack.lastOrNull()
    if (from != null) {
        newBackstack.removeAt(newBackstack.lastIndex)
    }
    newBackstack.add(transaction)
    setBackstack(newBackstack, transaction.pushChangeHandler)
}


/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(changeHandler: ControllerChangeHandler? = null): Boolean {
    val rootTransaction = backstack.firstOrNull()
    return if (rootTransaction != null) {
        popToTransaction(rootTransaction, changeHandler)
        true
    } else {
        false
    }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, changeHandler: ControllerChangeHandler? = null): Boolean {
    val transaction = backstack.firstOrNull { it.tag == tag } ?: return false
    popToTransaction(transaction, changeHandler)
    return true
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popToController(
    controller: Controller,
    changeHandler: ControllerChangeHandler? = null
): Boolean {
    val transaction =
        backstack.firstOrNull { it.controller == controller } ?: return false
    popToTransaction(transaction, changeHandler)
    return true
}

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
fun Router.setRoot(transaction: RouterTransaction) {
    setBackstack(listOf(transaction), transaction.pushChangeHandler)
}