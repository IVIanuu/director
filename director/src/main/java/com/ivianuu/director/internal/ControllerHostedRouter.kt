package com.ivianuu.director.internal

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeListener
import com.ivianuu.director.ControllerLifecycleListener
import com.ivianuu.director.Router
import com.ivianuu.director.RouterTransaction

internal class ControllerHostedRouter : Router {

    override val activity: FragmentActivity
        get() = hostController.activity

    override val siblingRouters: List<Router>
        get() = hostController.let { it.childRouters + it.router.siblingRouters }

    override val rootRouter: Router
        get() = hostController.router.rootRouter

    override val transactionIndexer: TransactionIndexer
        get() {
            val rootRouter = rootRouter

            // something must be wrong here
            if (rootRouter == this) {
                val hostController = hostController
                val debugInfo =
                    "${hostController.javaClass.simpleName}, ${hostController.isAttached}, ${hostController.isBeingDestroyed}, ${hostController.parentController}"
                throw IllegalStateException("Unable to retrieve TransactionIndexer from $debugInfo")
            } else {
                return rootRouter.transactionIndexer
            }
        }

    private val hostController: Controller

    var hostId = 0
        private set

    var tag: String? = null
        private set

    var isDetachFrozen = false
        set(value) {
            field = value
            backstack.forEach { it.controller.isDetachFrozen = value }
        }

    constructor(hostController: Controller) {
        this.hostController = hostController
    }

    constructor(hostController: Controller, hostId: Int, tag: String?) {
        this.hostController = hostController
        this.hostId = hostId
        this.tag = tag
    }

    override fun getAllChangeListeners(recursiveOnly: Boolean): List<ControllerChangeListener> {
        val listeners =
            super.getAllChangeListeners(recursiveOnly).toMutableList()

        hostController.router.let { listeners.addAll(it.getAllChangeListeners(true)) }

        return listeners
    }

    override fun getAllLifecycleListeners(recursiveOnly: Boolean): List<ControllerLifecycleListener> {
        val listeners =
            super.getAllLifecycleListeners(recursiveOnly).toMutableList()

        hostController.router.let { listeners.addAll(it.getAllLifecycleListeners(true)) }

        return listeners
    }

    override fun destroy(popViews: Boolean) {
        isDetachFrozen = false
        super.destroy(popViews)
    }

    override fun pushToBackstack(entry: RouterTransaction) {
        if (isDetachFrozen) {
            entry.controller.isDetachFrozen = true
        }
        super.pushToBackstack(entry)
    }

    override fun setBackstack(
        newBackstack: List<RouterTransaction>,
        changeHandler: ControllerChangeHandler?
    ) {
        if (isDetachFrozen) {
            newBackstack.forEach { it.controller.isDetachFrozen = true }
        }
        super.setBackstack(newBackstack, changeHandler)
    }

    override fun onActivityDestroyed(activity: FragmentActivity) {
        super.onActivityDestroyed(activity)
        removeContainer(true)
    }

    fun saveBasicInstanceState(outState: Bundle) {
        outState.putInt(KEY_HOST_ID, hostId)
        outState.putString(KEY_TAG, tag)
    }

    fun restoreBasicInstanceState(savedInstanceState: Bundle) {
        hostId = savedInstanceState.getInt(KEY_HOST_ID)
        tag = savedInstanceState.getString(KEY_TAG)
    }

    override fun setControllerRouter(controller: Controller) {
        controller.parentController = hostController
        super.setControllerRouter(controller)
    }

    override fun getRetainedObjects(instanceId: String) =
        hostController.router.getRetainedObjects(instanceId)

    override fun startActivity(intent: Intent) {
        hostController.router.startActivity(intent)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int
    ) {
        hostController.router.startActivityForResult(instanceId, intent, requestCode)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        hostController.router.startActivityForResult(
            instanceId, intent, requestCode, options
        )
    }

    override fun startIntentSenderForResult(
        instanceId: String,
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    ) {
        hostController.router.startIntentSenderForResult(
            instanceId, intent, requestCode,
            fillInIntent, flagsMask, flagsValues, extraFlags, options
        )
    }

    override fun registerForActivityResult(instanceId: String, requestCode: Int) {
        hostController.router.registerForActivityResult(instanceId, requestCode)
    }

    override fun unregisterForActivityResults(instanceId: String) {
        hostController.router.unregisterForActivityResults(instanceId)
    }

    override fun requestPermissions(
        instanceId: String,
        permissions: Array<String>,
        requestCode: Int
    ) {
        hostController.router.requestPermissions(instanceId, permissions, requestCode)
    }

    fun removeContainer(forceViewRemoval: Boolean) {
        destroyingControllers.toList()
            .filter { it.view != null }
            .forEach { it.detach(it.view!!, true, false, true, true) }

        backstack
            .filter { it.controller.view != null }
            .forEach {
                it.controller.detach(
                    it.controller.view!!,
                    forceViewRemoval,
                    false,
                    forceViewRemoval,
                    true
                )
            }

        prepareForContainerRemoval()
        container = null
    }

    private companion object {
        private const val KEY_HOST_ID = "ControllerHostedRouter.hostId"
        private const val KEY_TAG = "ControllerHostedRouter.tag"
    }
}
