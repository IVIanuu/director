package com.ivianuu.director

import android.os.Bundle
import com.ivianuu.director.internal.TransactionIndexer

/**
 * Metadata used for adding [Controller]s to a [Router].
 */
class RouterTransaction internal constructor(
    val controller: Controller,
    val tag: String?,
    val pushChangeHandler: ControllerChangeHandler?,
    val popChangeHandler: ControllerChangeHandler?,
    internal var transactionIndex: Int = INVALID_INDEX,
    internal var attachedToRouter: Boolean = false
) {

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

    internal companion object {
        private const val KEY_CONTROLLER_BUNDLE = "RouterTransaction.controller.bundle"
        private const val KEY_PUSH_CHANGE_HANDLER = "RouterTransaction.pushChangeHandler"
        private const val KEY_POP_CHANGE_HANDLER = "RouterTransaction.popChangeHandler"
        private const val KEY_TAG = "RouterTransaction.tag"
        private const val KEY_INDEX = "RouterTransaction.transactionIndex"
        private const val KEY_ATTACHED_TO_ROUTER = "RouterTransaction.attachedToRouter"

        const val INVALID_INDEX = -1

        fun fromBundle(
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

fun RouterTransaction(
    controller: Controller,
    pushHandler: ControllerChangeHandler? = DirectorPlugins.defaultPushHandler,
    popHandler: ControllerChangeHandler? = DirectorPlugins.defaultPopHandler,
    tag: String? = null
): RouterTransaction = RouterTransaction(
    controller, tag, pushHandler,
    popHandler, RouterTransaction.INVALID_INDEX, false
)