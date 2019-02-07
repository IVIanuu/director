package com.ivianuu.director

import android.os.Bundle

import com.ivianuu.director.internal.TransactionIndexer
import kotlin.properties.Delegates

/**
 * Metadata used for adding [Controller]s to a [Router].
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
        internal set

    /**
     * The push change changeHandler of this transaction
     */
    var pushChangeHandler: ControllerChangeHandler? = null
        internal set

    /**
     * The pop change changeHandler of this transaction
     */
    var popChangeHandler: ControllerChangeHandler? = null
        internal set

    internal var transactionIndex = INVALID_INDEX
    internal var attachedToRouter = false

    internal constructor(
        controller: Controller,
        tag: String?,
        pushHandler: ControllerChangeHandler?,
        popHandler: ControllerChangeHandler?
    ) {
        this.controller = controller
        this.tag = tag
        this.pushChangeHandler = pushHandler
        this.popChangeHandler = popHandler
    }

    private constructor(
        controller: Controller,
        tag: String?,
        pushHandler: ControllerChangeHandler?,
        popHandler: ControllerChangeHandler?,
        transactionIndex: Int,
        attachedToRouter: Boolean
    ) {
        this.controller = controller
        this.tag = tag
        this.pushChangeHandler = pushHandler
        this.popChangeHandler = popHandler
        this.transactionIndex = transactionIndex
        this.attachedToRouter = attachedToRouter
    }

    internal fun ensureValidIndex(indexer: TransactionIndexer) {
        if (transactionIndex == INVALID_INDEX) {
            transactionIndex = indexer.nextIndex()
        }
    }

    /**
     * Used to serialize this transaction into a Bundle
     */
    internal fun saveInstanceState() = Bundle().apply {
        putBundle(KEY_CONTROLLER_BUNDLE, controller.saveInstanceState())
        pushChangeHandler?.let { putBundle(KEY_PUSH_CHANGE_HANDLER, it.toBundle()) }
        popChangeHandler?.let { putBundle(KEY_POP_CHANGE_HANDLER, it.toBundle()) }
        putString(KEY_TAG, tag)
        putInt(KEY_INDEX, transactionIndex)
        putBoolean(KEY_ATTACHED_TO_ROUTER, attachedToRouter)
    }

    companion object {
        private const val KEY_CONTROLLER_BUNDLE = "RouterTransaction.controller.bundle"
        private const val KEY_PUSH_CHANGE_HANDLER = "RouterTransaction.pushChangeHandler"
        private const val KEY_POP_CHANGE_HANDLER = "RouterTransaction.popChangeHandler"
        private const val KEY_TAG = "RouterTransaction.tag"
        private const val KEY_INDEX = "RouterTransaction.transactionIndex"
        private const val KEY_ATTACHED_TO_ROUTER = "RouterTransaction.attachedToRouter"

        private const val INVALID_INDEX = -1

        internal fun fromBundle(
            bundle: Bundle,
            controllerFactory: ControllerFactory
        ) = RouterTransaction(
                Controller.fromBundle(bundle.getBundle(KEY_CONTROLLER_BUNDLE)!!, controllerFactory),
            bundle.getString(KEY_TAG),
            bundle.getBundle(KEY_PUSH_CHANGE_HANDLER)?.let { ControllerChangeHandler.fromBundle(it) },
            bundle.getBundle(KEY_POP_CHANGE_HANDLER)?.let { ControllerChangeHandler.fromBundle(it) },
            bundle.getInt(KEY_INDEX),
            bundle.getBoolean(KEY_ATTACHED_TO_ROUTER)
        )
    }
}

class RouterTransactionBuilder @PublishedApi internal constructor() {

    var controller by Delegates.notNull<Controller>()
        private set
    var tag: String? = null
        private set
    var pushChangeHandler: ControllerChangeHandler? = null
        private set
    var popChangeHandler: ControllerChangeHandler? = null
        private set

    fun controller(controller: Controller): RouterTransactionBuilder = apply {
        this.controller = controller
    }

    fun tag(tag: String?): RouterTransactionBuilder = apply {
        this.tag = tag
    }

    fun pushHandler(handler: ControllerChangeHandler?): RouterTransactionBuilder = apply {
        this.pushChangeHandler = handler
    }

    fun popHandler(handler: ControllerChangeHandler?): RouterTransactionBuilder = apply {
        this.popChangeHandler = handler
    }

    fun build(): RouterTransaction {
        return RouterTransaction(
            controller, tag, pushChangeHandler, popChangeHandler
        )
    }
}

/**
 * Returns a new [RouterTransaction] build by [init]
 */
fun transaction(
    controller: Controller,
    pushHandler: ControllerChangeHandler? = null,
    popHandler: ControllerChangeHandler? = null,
    tag: String? = null
): RouterTransaction = transaction {
    controller(controller)
    pushHandler(pushHandler)
    popHandler(popHandler)
    tag(tag)
}

/**
 * Returns a new [RouterTransaction] build by [init]
 */
inline fun transaction(
    init: RouterTransactionBuilder.() -> Unit
): RouterTransaction = RouterTransactionBuilder().apply(init).build()

/**
 * Sets the [handler] as the push and pop change handler
 */
fun RouterTransactionBuilder.handler(handler: ControllerChangeHandler?) =
    pushHandler(handler).popHandler(handler)