package com.ivianuu.director.internal

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.Router

internal class RootRouter(
    override val activity: Activity,
    container: ViewGroup
) : Router() {

    override val containerId = container.id

    override val transactionIndexer = TransactionIndexer()

    override val rootRouter: Router
        get() = this

    init {
        this.container = container
    }

    override fun saveInstanceState(): Bundle {
        return super.saveInstanceState().also {
            it.putBundle(KEY_TRANSACTION_INDEXER, transactionIndexer.saveInstanceState())
        }
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        super.restoreInstanceState(savedInstanceState)
        transactionIndexer.restoreInstanceState(
            savedInstanceState.getBundle(KEY_TRANSACTION_INDEXER)!!
        )
    }

    private companion object {
        private const val KEY_TRANSACTION_INDEXER = "RootRouter.transactionIndexer"
    }
}