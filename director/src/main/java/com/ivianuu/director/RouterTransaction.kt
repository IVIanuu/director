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
import com.ivianuu.director.internal.TransactionIndexer

/**
 * Describes a transaction in a [Router]
 */
class RouterTransaction {

    /**
     * The controller of this transaction
     */
    val controller: Controller

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

    internal var transactionIndex = INVALID_INDEX
    internal var isAttachedToRouter = false

    constructor(controller: Controller) {
        this.controller = controller
    }

    private constructor(
        controller: Controller,
        tag: String?,
        pushChangeHandler: ControllerChangeHandler?,
        popChangeHandler: ControllerChangeHandler?,
        transactionIndex: Int,
        attachedToRouter: Boolean
    ) {
        this.controller = controller
        this.pushChangeHandler = pushChangeHandler
        this.popChangeHandler = popChangeHandler
        this.tag = tag
        this.transactionIndex = transactionIndex
        this.isAttachedToRouter = attachedToRouter
    }

    internal fun ensureValidIndex(indexer: TransactionIndexer) {
        if (transactionIndex == INVALID_INDEX) {
            transactionIndex = indexer.nextIndex()
        }
    }

    private fun checkModify() {
        check(!isAttachedToRouter) {
            "transactions cannot be modified after being added to a Router."
        }
    }

    internal fun saveInstanceState(): Bundle {
        return Bundle().apply {
            putBundle(KEY_CONTROLLER_BUNDLE, controller.saveInstanceState())
            pushChangeHandler?.let { putBundle(KEY_PUSH_CHANGE_HANDLER, it.toBundle()) }
            popChangeHandler?.let { putBundle(KEY_POP_CHANGE_HANDLER, it.toBundle()) }
            putString(KEY_TAG, tag)
            putInt(KEY_INDEX, transactionIndex)
            putBoolean(KEY_ATTACHED_TO_ROUTER, isAttachedToRouter)
        }
    }

    internal companion object {
        private const val KEY_CONTROLLER_BUNDLE = "RouterTransaction.controller.bundle"
        private const val KEY_PUSH_CHANGE_HANDLER = "RouterTransaction.pushChangeHandler"
        private const val KEY_POP_CHANGE_HANDLER = "RouterTransaction.popChangeHandler"
        private const val KEY_TAG = "RouterTransaction.tag"
        private const val KEY_INDEX = "RouterTransaction.transactionIndex"
        private const val KEY_ATTACHED_TO_ROUTER = "RouterTransaction.isAttachedToRouter"

        const val INVALID_INDEX = -1

        fun fromBundle(
            bundle: Bundle,
            controllerFactory: ControllerFactory
        ): RouterTransaction {
            return RouterTransaction(
                Controller.fromBundle(bundle.getBundle(KEY_CONTROLLER_BUNDLE)!!, controllerFactory),
                bundle.getString(KEY_TAG),
                bundle.getBundle(KEY_PUSH_CHANGE_HANDLER)?.let(ControllerChangeHandler.Companion::fromBundle),
                bundle.getBundle(KEY_POP_CHANGE_HANDLER)?.let(ControllerChangeHandler.Companion::fromBundle),
                bundle.getInt(KEY_INDEX),
                bundle.getBoolean(KEY_ATTACHED_TO_ROUTER)
            )
        }
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