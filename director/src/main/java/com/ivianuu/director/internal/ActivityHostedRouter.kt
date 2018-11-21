package com.ivianuu.director.internal

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Router

internal class ActivityHostedRouter(
    private val lifecycleHandler: LifecycleHandler,
    container: ViewGroup
) : Router() {

    override val transactionIndexer = TransactionIndexer()

    override val activity: FragmentActivity

    override val siblingRouters: List<Router>
        get() = lifecycleHandler.routers

    override val rootRouter: Router
        get() = this

    init {
        this.container = container
        this.activity = lifecycleHandler.requireActivity()
    }

    override fun saveInstanceState(outState: Bundle) {
        super.saveInstanceState(outState)
        transactionIndexer.saveInstanceState(outState)
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        super.restoreInstanceState(savedInstanceState)
        transactionIndexer.restoreInstanceState(savedInstanceState)
    }

    override fun getRetainedObjects(instanceId: String) = lifecycleHandler
        .getRetainedObjects(instanceId)

    override fun removeRetainedObjects(instanceId: String) {
        lifecycleHandler.removeRetainedObjects(instanceId)
    }

    override fun startActivity(intent: Intent) {
        lifecycleHandler.startActivity(intent)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int
    ) {
        lifecycleHandler.startActivityForResult(instanceId, intent, requestCode)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        lifecycleHandler.startActivityForResult(instanceId, intent, requestCode, options)
    }

    override fun startIntentSenderForResult(
        instanceId: String, intent: IntentSender, requestCode: Int, fillInIntent: Intent?,
        flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?
    ) {
        lifecycleHandler.startIntentSenderForResult(
            instanceId,
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
            options
        )
    }

    override fun registerForActivityResult(instanceId: String, requestCode: Int) {
        lifecycleHandler.registerForActivityResult(instanceId, requestCode)
    }

    override fun unregisterForActivityResults(instanceId: String) {
        lifecycleHandler.unregisterForActivityResults(instanceId)
    }

    override fun requestPermissions(
        instanceId: String,
        permissions: Array<String>,
        requestCode: Int
    ) {
        lifecycleHandler.requestPermissions(instanceId, permissions, requestCode)
    }
}
