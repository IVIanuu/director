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

/**
 * Describes a transaction in a [router]
 */
class RouterTransaction(
    /**
     * The controller of this transaction
     */
    val controller: Controller
) {

    /**
     * The tag of this transaction
     */
    var tag: String? = null
        set(value) {
            checkModify()
            field = value
        }

    /**
     * Will be used when this transaction gets pushed
     */
    var pushChangeHandler: ControllerChangeHandler? = DirectorPlugins.defaultPushHandler
        set(value) {
            checkModify()
            field = value
        }

    /**
     * Will be called when this transaction gets popped
     */
    var popChangeHandler: ControllerChangeHandler? = DirectorPlugins.defaultPopHandler
        set(value) {
            checkModify()
            field = value
        }

    var transactionIndex = INVALID_INDEX
        internal set
    internal var isAddedToRouter = false

    internal fun ensureValidIndex() {
        if (transactionIndex == INVALID_INDEX) {
            transactionIndex = TransactionIndexer.nextIndex()
        }
    }

    private fun checkModify() {
        check(!isAddedToRouter) {
            "transactions cannot be modified after being added to a router."
        }
    }

    internal companion object {
        const val INVALID_INDEX = -1
    }
}

fun RouterTransaction.pushChangeHandler(changeHandler: ControllerChangeHandler?): RouterTransaction =
    apply {
        pushChangeHandler = changeHandler
    }

fun RouterTransaction.popChangeHandler(changeHandler: ControllerChangeHandler?): RouterTransaction =
    apply {
        popChangeHandler = changeHandler
    }

fun RouterTransaction.changeHandler(changeHandler: ControllerChangeHandler?) =
    pushChangeHandler(changeHandler).popChangeHandler(changeHandler)

fun RouterTransaction.tag(tag: String?): RouterTransaction = apply {
    this.tag = tag
}