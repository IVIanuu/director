package com.ivianuu.director

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.internal.ChangeTransaction
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.director.internal.DefaultControllerFactory
import com.ivianuu.director.internal.NoOpControllerChangeHandler
import com.ivianuu.director.internal.TransactionIndexer
import com.ivianuu.director.internal.backstacksAreEqual
import com.ivianuu.director.internal.filterVisible

/**
 * A Router implements navigation and backstack handling for [Controller]s. Router objects are attached
 * to Activity/containing ViewGroup pairs. Routers do not directly render or push Views to the container ViewGroup,
 * but instead defer this responsibility to the [ControllerChangeHandler] specified in a given transaction.
 */
abstract class Router {

    /**
     * The current backstack, ordered from root to most recently pushed.
     */
    val backstack get() = _backstack.toList()
    private val _backstack = mutableListOf<RouterTransaction>()
    private val reversedBackstack get() = _backstack.reversed()

    /**
     * Returns this Router's host Activity or `null` if it has either not yet been attached to
     * an Activity or if the Activity has been destroyed.
     */
    abstract val activity: FragmentActivity

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = false

    internal var container: ViewGroup? = null
        set(value) {
            if (value == field) return

            (field as? ControllerChangeListener)?.let { removeChangeListener(it) }
            (value as? ControllerChangeListener)?.let { addChangeListener(it) }

            field = value
        }

    internal val hasContainer get() = container != null

    /**
     * Will be used to instantiate controllers after process death
     */
    var controllerFactory: ControllerFactory?
        get() = _controllerFactory
        set(value) {
            _controllerFactory = value ?: DefaultControllerFactory()
        }

    private var _controllerFactory: ControllerFactory = DefaultControllerFactory()

    internal abstract val siblingRouters: List<Router>
    internal abstract val rootRouter: Router
    internal abstract val transactionIndexer: TransactionIndexer

    private val changeManager = ControllerChangeManager()

    private val changeListeners = mutableListOf<ChangeListenerEntry>()
    private val lifecycleListeners = mutableListOf<LifecycleListenerEntry>()
    protected val destroyingControllers = mutableListOf<Controller>()

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ControllerChangeHandler]
     */
    open fun setBackstack(
        newBackstack: List<RouterTransaction>,
        changeHandler: ControllerChangeHandler? = null
    ) {
        val oldTransactions = backstack
        val oldVisibleTransactions = oldTransactions.filterVisible()

        // Swap around transaction indices to ensure they don't get thrown out of order by the
        // developer rearranging the backstack at runtime.
        val indices = newBackstack
            .onEach { it.ensureValidIndex(transactionIndexer) }
            .map { it.transactionIndex }
            .sorted()

        newBackstack.forEachIndexed { i, transaction ->
            transaction.transactionIndex = indices[i]
        }

        if (newBackstack.size != newBackstack.distinctBy { it.controller }.size) {
            throw IllegalStateException("Trying to push the same controller to the backstack more than once.")
        }

        _backstack.clear()
        _backstack.addAll(newBackstack)

        val transactionsToBeRemoved = oldTransactions
            .filter { old -> newBackstack.none { it.controller == old.controller } }
            .onEach {
                // Inform the controller that it will be destroyed soon
                it.controller.isBeingDestroyed = true
            }

        // Ensure all new controllers have a valid router set
        newBackstack.forEach {
            it.attachedToRouter = true
            setControllerRouter(it.controller)
        }

        if (newBackstack.isNotEmpty()) {
            val newVisibleTransactions = newBackstack.filterVisible()

            val newRootRequiresPush =
                newVisibleTransactions.isEmpty() ||
                        !oldTransactions.contains(newVisibleTransactions.first())

            val visibleTransactionsChanged =
                !backstacksAreEqual(newVisibleTransactions, oldVisibleTransactions)

            if (visibleTransactionsChanged) {
                val oldRootTransaction = oldVisibleTransactions.firstOrNull()
                val newRootTransaction = newVisibleTransactions.first()

                // Replace the old visible root with the new one
                if (oldRootTransaction == null || oldRootTransaction.controller != newRootTransaction.controller) {
                    // Ensure the existing root controller is fully pushed to the view hierarchy
                    if (oldRootTransaction != null) {
                        changeManager.completeChangeImmediately(oldRootTransaction.controller.instanceId)
                    }

                    performControllerChange(
                        newRootTransaction,
                        oldRootTransaction,
                        newRootRequiresPush,
                        changeHandler
                    )
                }

                // Remove all visible controllers that were previously on the backstack
                oldVisibleTransactions
                    .drop(1)
                    .reversed()
                    .filterNot { newVisibleTransactions.contains(it) }
                    .forEach {
                        val localHandler = changeHandler?.copy() ?: SimpleSwapChangeHandler()
                        localHandler.forceRemoveViewOnPush = true
                        changeManager.completeChangeImmediately(it.controller.instanceId)
                        performControllerChange(
                            null,
                            it,
                            newRootRequiresPush,
                            localHandler
                        )
                    }

                // Add any new controllers to the backstack
                newVisibleTransactions
                    .drop(1)
                    .filterNot { oldVisibleTransactions.contains(it) }
                    .forEach {
                        performControllerChange(
                            it,
                            newVisibleTransactions[newVisibleTransactions.indexOf(it) - 1],
                            true,
                            it.pushChangeHandler
                        )
                    }
            }
        } else {
            // Remove all visible controllers that were previously on the backstack
            oldVisibleTransactions.reversed().forEach {
                val localHandler = changeHandler?.copy() ?: SimpleSwapChangeHandler()
                changeManager.completeChangeImmediately(it.controller.instanceId)
                performControllerChange(null, it, false, localHandler)
            }
        }

        // Destroy all old controllers that are no longer on the backstack. We don't do this when we initially
        // set the backstack to prevent the possibility that they'll be destroyed before the controller
        // change handler runs.
        transactionsToBeRemoved.forEach { it.controller.destroy() }
    }

    /**
     * Attaches this Router's existing backstack to its container if one exists.
     */
    open fun rebindIfNeeded() {
        backstack
            .filterVisible()
            .forEach {
                performControllerChange(it, null, true, SimpleSwapChangeHandler(false))
            }
    }

    /**
     * Adds a listener for all of this Router's [Controller] change events
     */
    fun addChangeListener(listener: ControllerChangeListener, recursive: Boolean = false) {
        if (!getAllChangeListeners(false).contains(listener)) {
            changeListeners.add(ChangeListenerEntry(listener, recursive))
        }
    }

    /**
     * Removes a previously added listener
     */
    fun removeChangeListener(listener: ControllerChangeListener) {
        changeListeners.removeAll { it.listener == listener }
    }

    internal open fun getAllChangeListeners(recursiveOnly: Boolean) =
        changeListeners
            .filter { !recursiveOnly || it.recursive }
            .map { it.listener }

    /**
     * Adds a lifecycle listener for controllers of this router
     */
    fun addLifecycleListener(listener: ControllerLifecycleListener, recursive: Boolean = false) {
        if (!getAllLifecycleListeners(false).contains(listener)) {
            lifecycleListeners.add(LifecycleListenerEntry(listener, recursive))
        }
    }

    /**
     * Removes a previously added listener
     */
    fun removeLifecycleListener(listener: ControllerLifecycleListener) {
        lifecycleListeners.removeAll { it.listener == listener }
    }

    internal fun getAllLifecycleListeners() =
        getAllLifecycleListeners(false)

    internal open fun getAllLifecycleListeners(recursiveOnly: Boolean) =
        lifecycleListeners
            .filter { !recursiveOnly || it.recursive }
            .map { it.listener }

    open fun onActivityResult(
        instanceIds: Set<String>,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        instanceIds
            .mapNotNull { findControllerByInstanceId(it) }
            .forEach { it.onActivityResult(requestCode, resultCode, data) }
    }

    open fun onRequestPermissionsResult(
        instanceIds: Set<String>,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        instanceIds
            .mapNotNull { findControllerByInstanceId(it) }
            .forEach { it.onRequestPermissionsResult(requestCode, permissions, grantResults) }
    }

    open fun onActivityStarted() {
        reversedBackstack.forEach { it.controller.activityStarted() }
    }

    open fun onActivityResumed() {
        reversedBackstack.forEach { it.controller.activityResumed() }
    }

    open fun onActivityPaused() {
        reversedBackstack.forEach { it.controller.activityPaused() }
    }

    open fun onActivityStopped() {
        reversedBackstack.forEach { it.controller.activityStopped() }
    }

    open fun onActivityDestroyed() {
        reversedBackstack.forEach { it.controller.activityDestroyed() }
        destroyingControllers.reversed().forEach { it.activityDestroyed() }
        container = null
    }

    open fun prepareForHostDetach() {
        reversedBackstack.forEach {
            changeManager.completeChangeImmediately(it.controller.instanceId)
            it.controller.prepareForHostDetach()
        }
    }

    internal open fun destroy(popViews: Boolean) {
        popsLastView = true

        val poppedControllers = _backstack.reversed()
        _backstack.clear()

        poppedControllers
            .onEach { it.controller.destroy() }
            .onEach { trackDestroyingController(it) }

        if (popViews && poppedControllers.isNotEmpty()) {
            val topTransaction = poppedControllers.first()
            topTransaction.controller.addLifecycleListener(object : ControllerLifecycleListener {
                override fun onChangeEnd(
                    controller: Controller,
                    changeHandler: ControllerChangeHandler,
                    changeType: ControllerChangeType
                ) {
                    if (changeType == ControllerChangeType.POP_EXIT) {
                        poppedControllers
                            .drop(1)
                            .reversed()
                            .forEach {
                                performControllerChange(
                                    null,
                                    it,
                                    true,
                                    SimpleSwapChangeHandler()
                                )
                            }
                    }
                }
            })

            performControllerChange(null, topTransaction, false, topTransaction.popChangeHandler)
        }
    }

    open fun saveInstanceState(outState: Bundle) {
        val backstack = backstack.map { it.saveInstanceState() }
        outState.putParcelableArrayList(KEY_BACKSTACK, ArrayList(backstack))
        outState.putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
    }

    open fun restoreInstanceState(savedInstanceState: Bundle) {
        _backstack.clear()
        _backstack.addAll(
            savedInstanceState.getParcelableArrayList<Bundle>(KEY_BACKSTACK)!!
                .map { RouterTransaction.fromBundle(it, _controllerFactory) }
        )
        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        backstack.forEach { setControllerRouter(it.controller) }
    }

    fun shouldShowRequestPermissionRationale(permission: String, instanceIds: Set<String>) =
        instanceIds
        .mapNotNull { findControllerByInstanceId(it) }
        .filter { it.shouldShowRequestPermissionRationale(permission) }
        .any()

    private fun performControllerChange(
        to: RouterTransaction?,
        from: RouterTransaction?,
        isPush: Boolean,
        changeHandler: ControllerChangeHandler? = null
    ) {
        if (isPush && to != null) {
            to.attachedToRouter = true
        }

        var changeHandler = changeHandler ?: when {
            isPush -> to?.pushChangeHandler
            from != null -> from.popChangeHandler
            else -> null
        }

        val toController = to?.controller
        val fromController = from?.controller

        var forceDetachDestroy = false

        if (toController == null && _backstack.size == 0 && !popsLastView) {
            // We're emptying out the backstack. Views get weird if you transition them out, so just no-op it. The host
            // Activity or controller should be handling this by finishing or at least hiding this view.
            changeHandler = NoOpControllerChangeHandler()
            forceDetachDestroy = true
        }

        if (isPush && to != null && to.controller.isDestroyed) {
            throw IllegalStateException("Trying to push a controller that has already been destroyed ${to.javaClass.simpleName}")
        }

        val transaction = ChangeTransaction(
            to?.controller,
            from?.controller,
            isPush,
            container,
            changeHandler,
            getAllChangeListeners(false)
        )

        changeManager.executeChange(transaction)

        val fromView = fromController?.view
        if (forceDetachDestroy && fromView != null) {
            fromController.detach(fromView, true, false, true, false)
        }
    }

    protected open fun setControllerRouter(controller: Controller) {
        controller.setRouter(this)
    }

    private fun trackDestroyingController(transaction: RouterTransaction) {
        if (!transaction.controller.isDestroyed) {
            destroyingControllers.add(transaction.controller)

            transaction.controller.addLifecycleListener(object : ControllerLifecycleListener {
                override fun postDestroy(controller: Controller) {
                    destroyingControllers.remove(controller)
                }
            })
        }
    }

    internal abstract fun getRetainedObjects(instanceId: String): RetainedObjects

    internal abstract fun removeRetainedObjects(instanceId: String)

    internal abstract fun startActivity(intent: Intent)
    internal abstract fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int
    )

    internal abstract fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    )

    internal abstract fun startIntentSenderForResult(
        instanceId: String,
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    )

    internal abstract fun registerForActivityResult(instanceId: String, requestCode: Int)
    internal abstract fun unregisterForActivityResults(instanceId: String)
    internal abstract fun requestPermissions(
        instanceId: String,
        permissions: Array<String>,
        requestCode: Int
    )

    private data class ChangeListenerEntry(
        val listener: ControllerChangeListener,
        val recursive: Boolean
    )

    private data class LifecycleListenerEntry(
        val listener: ControllerLifecycleListener,
        val recursive: Boolean
    )

    companion object {
        private const val KEY_BACKSTACK = "Router.backstack"
        private const val KEY_POPS_LAST_VIEW = "Router.popsLastView"
    }
}