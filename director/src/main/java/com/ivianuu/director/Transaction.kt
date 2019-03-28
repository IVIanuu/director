package com.ivianuu.director

import android.os.Bundle
import com.ivianuu.director.internal.TransactionIndexer

/**
 * Describes a transaction in a [Router]
 */
class Transaction {

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
    var pushChangeHandler: ChangeHandler? = DirectorPlugins.defaultPushHandler
        set(value) {
            checkModify()
            field = value
        }

    /**
     * Will be called when this transaction gets popped
     */
    var popChangeHandler: ChangeHandler? = DirectorPlugins.defaultPopHandler
        set(value) {
            checkModify()
            field = value
        }

    internal var transactionIndex = INVALID_INDEX
    internal var attachedToRouter = false

    constructor(controller: Controller) {
        this.controller = controller
    }

    private constructor(
        controller: Controller,
        tag: String?,
        pushChangeHandler: ChangeHandler?,
        popChangeHandler: ChangeHandler?,
        transactionIndex: Int,
        attachedToRouter: Boolean
    ) {
        this.controller = controller
        this.pushChangeHandler = pushChangeHandler
        this.popChangeHandler = popChangeHandler
        this.tag = tag
        this.transactionIndex = transactionIndex
        this.attachedToRouter = attachedToRouter
    }

    internal fun ensureValidIndex(indexer: TransactionIndexer) {
        if (transactionIndex == INVALID_INDEX) {
            transactionIndex = indexer.nextIndex()
        }
    }

    private fun checkModify() {
        check(!attachedToRouter) {
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
            putBoolean(KEY_ATTACHED_TO_ROUTER, attachedToRouter)
        }
    }

    internal companion object {
        private const val KEY_CONTROLLER_BUNDLE = "Transaction.controller.bundle"
        private const val KEY_PUSH_CHANGE_HANDLER = "Transaction.pushChangeHandler"
        private const val KEY_POP_CHANGE_HANDLER = "Transaction.popChangeHandler"
        private const val KEY_TAG = "Transaction.tag"
        private const val KEY_INDEX = "Transaction.transactionIndex"
        private const val KEY_ATTACHED_TO_ROUTER = "Transaction.attachedToRouter"

        const val INVALID_INDEX = -1

        fun fromBundle(
            bundle: Bundle,
            controllerFactory: ControllerFactory
        ): Transaction {
            return Transaction(
                Controller.fromBundle(bundle.getBundle(KEY_CONTROLLER_BUNDLE)!!, controllerFactory),
                bundle.getString(KEY_TAG),
                bundle.getBundle(KEY_PUSH_CHANGE_HANDLER)?.let(ChangeHandler.Companion::fromBundle),
                bundle.getBundle(KEY_POP_CHANGE_HANDLER)?.let(ChangeHandler.Companion::fromBundle),
                bundle.getInt(KEY_INDEX),
                bundle.getBoolean(KEY_ATTACHED_TO_ROUTER)
            )
        }
    }
}

fun Transaction.pushChangeHandler(changeHandler: ChangeHandler?): Transaction =
    apply {
        pushChangeHandler = changeHandler
    }

fun Transaction.popChangeHandler(changeHandler: ChangeHandler?): Transaction =
    apply {
        popChangeHandler = changeHandler
    }

fun Transaction.changeHandler(changeHandler: ChangeHandler?) =
    pushChangeHandler(changeHandler).popChangeHandler(changeHandler)

fun Transaction.tag(tag: String?): Transaction = apply {
    this.tag = tag
}