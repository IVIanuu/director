package com.ivianuu.director.internal

import android.app.Activity
import android.os.Bundle

import com.ivianuu.director.Controller
import com.ivianuu.director.Router

internal class ChildRouter : Router {

    override val activity: Activity
        get() = hostController.activity

    override val rootRouter: Router
        get() = hostController.router.rootRouter

    override val transactionIndexer: TransactionIndexer
        get() = rootRouter.transactionIndexer

    private val hostController: Controller

    var hostId = 0
        private set

    var tag: String? = null
        private set

    constructor(hostController: Controller) {
        this.hostController = hostController
    }

    constructor(hostController: Controller, hostId: Int, tag: String?) {
        this.hostController = hostController
        this.hostId = hostId
        this.tag = tag
    }

    override fun getAllChangeListeners(recursiveOnly: Boolean) =
        super.getAllChangeListeners(recursiveOnly) +
                hostController.router.getAllChangeListeners(true)

    override fun getAllLifecycleListeners(recursiveOnly: Boolean) =
        super.getAllLifecycleListeners(recursiveOnly) +
                hostController.router.getAllLifecycleListeners(true)

    override fun onActivityDestroyed() {
        super.onActivityDestroyed()
        removeContainer(true)
    }

    fun saveIdentity(outState: Bundle) {
        outState.putInt(KEY_HOST_ID, hostId)
        outState.putString(KEY_TAG, tag)
    }

    fun restoreIdentity(savedInstanceState: Bundle) {
        hostId = savedInstanceState.getInt(KEY_HOST_ID)
        tag = savedInstanceState.getString(KEY_TAG)
    }

    override fun setControllerRouter(controller: Controller) {
        // make sure to set the parent controller before the
        // router is set
        controller.parentController = hostController
        super.setControllerRouter(controller)
    }

    fun removeContainer(forceViewRemoval: Boolean) {
        destroyingControllers.toList()
            .filter { it.view != null }
            .forEach {
                it.detach(
                    it.view!!,
                    true,
                    false,
                    true,
                    true
                )
            }

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

        container = null
    }

    private companion object {
        private const val KEY_HOST_ID = "ChildRouter.hostId"
        private const val KEY_TAG = "ChildRouter.tag"
    }
}
