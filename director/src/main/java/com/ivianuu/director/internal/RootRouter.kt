package com.ivianuu.director.internal

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.Router

internal class RootRouter(
    override val activity: Activity,
    container: ViewGroup
) : Router() {

    override val transactionIndexer = TransactionIndexer()

    override val rootRouter: Router
        get() = this

    init {
        this.container = container
    }

    override fun saveInstanceState(outState: Bundle) {
        super.saveInstanceState(outState)
        transactionIndexer.saveInstanceState(outState)
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        super.restoreInstanceState(savedInstanceState)
        transactionIndexer.restoreInstanceState(savedInstanceState)
    }

}