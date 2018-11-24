package com.ivianuu.director

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.internal.Backstack
import com.ivianuu.director.internal.ChangeTransaction
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.director.internal.DefaultControllerFactory
import com.ivianuu.director.internal.NoOpControllerChangeHandler
import com.ivianuu.director.internal.TransactionIndexer
import com.ivianuu.director.internal.requireMainThread

/**
 * A Router implements navigation and backstack handling for [Controller]s. Router objects are attached
 * to Activity/containing ViewGroup pairs. Routers do not directly render or push Views to the container ViewGroup,
 * but instead defer this responsibility to the [ControllerChangeHandler] specified in a given transaction.
 */
abstract class Router {

    /**
     * The current backstack, ordered from root to most recently pushed.
     */
    val backstack get() = _backstack.entries
    private val _backstack = Backstack()

    private val reversedBackstack get() = _backstack.reversedEntries

    /**
     * Whether or not this router has root [Controller]
     */
    val hasRootController get() = _backstack.size > 0

    /**
     * Returns this Router's host Activity or `null` if it has either not yet been attached to
     * an Activity or if the Activity has been destroyed.
     */
    abstract val activity: FragmentActivity

    private val changeListeners = mutableListOf<ChangeListenerEntry>()
    private val lifecycleListeners = mutableListOf<LifecycleListenerEntry>()
    private val pendingControllerChanges = mutableListOf<ChangeTransaction>()
    protected val destroyingControllers = mutableListOf<Controller>()

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = false

    private var containerFullyAttached = false
    internal var isActivityStopped = false

    internal var container: ViewGroup? = null
        set(value) {
            if (value == field) return

            (field as? ControllerChangeListener)?.let { removeChangeListener(it) }
            (value as? ControllerChangeListener)?.let { addChangeListener(it) }

            field = value

            value?.post {
                containerFullyAttached = true
                performPendingControllerChanges()
            }
        }

    /**
     * The container id which will be used by this router
     */
    val containerId: Int
        get() = container?.id ?: 0
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

    /**
     * This should be called by the host Activity when its onBackPressed method is called. The call will be forwarded
     * to its top [Controller]. If that controller doesn't handle it, then it will be popped.
     */
    fun handleBack(): Boolean {
        requireMainThread()

        val currentTransaction = _backstack.peek()
        if (currentTransaction != null) {
            if (currentTransaction.controller.handleBack()) {
                return true
            } else if (popCurrentController()) {
                return true
            }
        }

        return false
    }

    /**
     * Pops the top [Controller] from the backstack
     */
    fun popCurrentController(changeHandler: ControllerChangeHandler? = null): Boolean {
        requireMainThread()

        val transaction = _backstack.peek()
            ?: throw IllegalStateException("Trying to pop the current controller when there are none on the backstack.")
        return popController(transaction.controller, changeHandler)
    }

    /**
     * Pops the passed [Controller] from the backstack
     */
    fun popController(
        controller: Controller,
        changeHandler: ControllerChangeHandler? = null
    ): Boolean {
        requireMainThread()

        val topTransaction = _backstack.peek()
        val poppingTopController =
            topTransaction != null && topTransaction.controller == controller

        if (poppingTopController) {
            trackDestroyingController(_backstack.pop().also { it.controller.destroy() })

            if (changeHandler != null) {
                performControllerChange(_backstack.peek(), topTransaction, false, changeHandler)
            } else {
                performControllerChange(_backstack.peek(), topTransaction, false)
            }
        } else {
            var removedTransaction: RouterTransaction? = null
            var nextTransaction: RouterTransaction? = null

            val backstack = reversedBackstack

            val topPushHandler = topTransaction?.pushChangeHandler

            val needsNextTransactionAttach =
                if (topPushHandler != null) !topPushHandler.removesFromViewOnPush else false

            for (transaction in backstack) {
                if (transaction.controller == controller) {
                    if (controller.isAttached) {
                        trackDestroyingController(transaction)
                    }

                    _backstack.remove(transaction)
                    removedTransaction = transaction
                } else if (removedTransaction != null) {
                    if (needsNextTransactionAttach && !transaction.controller.isAttached) {
                        nextTransaction = transaction
                    }
                    break
                }
            }

            if (removedTransaction != null) {
                if (changeHandler != null) {
                    performControllerChange(
                        nextTransaction,
                        removedTransaction,
                        false,
                        changeHandler
                    )
                } else {
                    performControllerChange(nextTransaction, removedTransaction, false)
                }
            }
        }

        return if (popsLastView) {
            topTransaction != null
        } else {
            !_backstack.isEmpty
        }
    }

    /**
     * Pushes a new [Controller] to the backstack
     */
    fun pushController(transaction: RouterTransaction) {
        requireMainThread()

        val from = _backstack.peek()
        pushToBackstack(transaction)
        performControllerChange(transaction, from, true)
    }

    /**
     * Replaces this Router's top [Controller] with a new [Controller]
     */
    fun replaceTopController(transaction: RouterTransaction) {
        requireMainThread()

        val topTransaction = _backstack.peek()
        if (!_backstack.isEmpty) {
            trackDestroyingController(_backstack.pop().also { it.controller.destroy() })
        }

        val handler = transaction.pushChangeHandler
        if (topTransaction != null) {
            val oldHandlerRemovedViews =
                topTransaction.pushChangeHandler?.removesFromViewOnPush == true

            val newHandlerRemovesViews = handler?.removesFromViewOnPush == true

            if (!oldHandlerRemovedViews && newHandlerRemovesViews) {
                backstack
                    .filterVisible()
                    .forEach { performControllerChange(null, it, true, handler) }
            }
        }

        pushToBackstack(transaction)

        handler?.forceRemoveViewOnPush = true

        performControllerChange(
            transaction.also { it.pushChangeHandler = handler },
            topTransaction,
            true
        )
    }

    internal open fun destroy(popViews: Boolean) {
        popsLastView = true

        val poppedControllers = _backstack.popAll()
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

    /**
     * Returns the hosted Controller with the given instance id or `null` if no such
     * Controller exists in this Router.
     */
    fun findControllerByInstanceId(instanceId: String) = backstack
        .mapNotNull { it.controller.findController(instanceId) }
        .firstOrNull()

    /**
     * Returns the hosted Controller that was pushed with the given tag or `null` if no
     * such Controller exists in this Router.
     */
    fun findControllerByTag(tag: String) =
        backstack.firstOrNull { it.tag == tag }?.controller

    /**
     * Pops all [Controller] until only the root is left
     */
    fun popToRoot(changeHandler: ControllerChangeHandler? = null): Boolean {
        requireMainThread()

        val rootTransaction = _backstack.root
        return if (rootTransaction != null) {
            popToTransaction(rootTransaction, changeHandler)
            true
        } else {
            false
        }
    }

    /**
     * Pops all [Controller]s until the [Controller] with the passed tag is at the top
     */
    fun popToTag(tag: String, changeHandler: ControllerChangeHandler? = null): Boolean {
        requireMainThread()
        val transaction = backstack.firstOrNull { it.tag == tag } ?: return false
        popToTransaction(transaction, changeHandler)
        return true
    }

    /**
     * Pops all [Controller]s until the [controller] is at the top
     */
    fun popToController(
        controller: Controller,
        changeHandler: ControllerChangeHandler? = null
    ): Boolean {
        requireMainThread()
        val transaction =
            backstack.firstOrNull { it.controller == controller } ?: return false
        popToTransaction(transaction, changeHandler)
        return true
    }

    /**
     * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
     */
    fun setRoot(transaction: RouterTransaction) {
        requireMainThread()
        setBackstack(listOf(transaction), transaction.pushChangeHandler)
    }

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ControllerChangeHandler]
     */
    open fun setBackstack(
        newBackstack: List<RouterTransaction>,
        changeHandler: ControllerChangeHandler? = null
    ) {
        requireMainThread()

        val oldTransactions = backstack
        val oldVisibleTransactions = oldTransactions.filterVisible()

        removeAllExceptVisibleAndUnowned()

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

        _backstack.setEntries(newBackstack)

        val transactionsToBeRemoved = oldTransactions
            .filter { old -> newBackstack.none { it.controller == old.controller } }
            .onEach {
                // Inform the controller that it will be destroyed soon
                it.controller.isBeingDestroyed = true
            }

        // Ensure all new controllers have a valid router set
        backstack.forEach {
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

                // Replace the old root with the new one
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

    /**
     * Saves the instance state of [controller] this can later be used in
     * [Controller.setInitialSavedState]
     */
    fun saveControllerInstanceState(controller: Controller): Bundle {
        if (backstack.none { it.controller == controller }) {
            throw IllegalArgumentException("controller is not attached to the router")
        }

        return controller.saveInstanceState()
    }

    internal fun onActivityResult(instanceIds: Set<String>, requestCode: Int, resultCode: Int, data: Intent?) {
        instanceIds
            .mapNotNull { findControllerByInstanceId(it) }
            .forEach { it.onActivityResult(requestCode, resultCode, data) }
    }

    internal fun onRequestPermissionsResult(
        instanceIds: Set<String>,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        instanceIds
            .mapNotNull { findControllerByInstanceId(it) }
            .forEach { it.onRequestPermissionsResult(requestCode, permissions, grantResults) }
    }

    /**
     * Attaches this Router's existing backstack to its container if one exists.
     */
    fun rebindIfNeeded() {
        requireMainThread()

        backstack
            .filter { it.controller.needsAttach }
            .forEach {
                performControllerChange(it, null, true, SimpleSwapChangeHandler(false))
            }
    }

    internal fun onActivityStarted(activity: FragmentActivity) {
        isActivityStopped = false

        reversedBackstack.forEach { transaction ->
            transaction.controller.activityStarted(activity)
            transaction.controller.childRouters.forEach { it.onActivityStarted(activity) }
        }
    }

    internal fun onActivityResumed(activity: FragmentActivity) {
        reversedBackstack.forEach { transaction ->
            transaction.controller.activityResumed(activity)
            transaction.controller.childRouters.forEach { it.onActivityResumed(activity) }
        }
    }

    internal fun onActivityPaused(activity: FragmentActivity) {
        reversedBackstack.forEach { transaction ->
            transaction.controller.activityPaused(activity)
            transaction.controller.childRouters.forEach { it.onActivityPaused(activity) }
        }
    }

    internal fun onActivityStopped(activity: FragmentActivity) {
        reversedBackstack.forEach { transaction ->
            transaction.controller.activityStopped(activity)
            transaction.controller.childRouters.forEach { it.onActivityStopped(activity) }
        }

        isActivityStopped = true
    }

    internal open fun onActivityDestroyed(activity: FragmentActivity) {
        prepareForContainerRemoval()

        reversedBackstack.forEach { transaction ->
            transaction.controller.activityDestroyed(activity)
            transaction.controller.childRouters.forEach { it.onActivityDestroyed(activity) }
        }

        destroyingControllers.reversed().forEach { controller ->
            controller.activityDestroyed(activity)
            controller.childRouters.forEach { it.onActivityDestroyed(activity) }
        }

        container = null
    }

    fun prepareForHostDetach() {
        reversedBackstack.forEach {
            if (changeManager.completeChangeImmediately(it.controller.instanceId)) {
                it.controller.needsAttach = true
            }
            it.controller.prepareForHostDetach()
        }
    }

    open fun saveInstanceState(outState: Bundle) {
        val backstackState = Bundle()
        _backstack.saveInstanceState(backstackState)

        outState.putParcelable(KEY_BACKSTACK, backstackState)
        outState.putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
    }

    open fun restoreInstanceState(savedInstanceState: Bundle) {
        val backstackBundle = savedInstanceState.getParcelable<Bundle>(KEY_BACKSTACK)!!
        _backstack.restoreInstanceState(backstackBundle, _controllerFactory)
        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        backstack.forEach { setControllerRouter(it.controller) }
    }

    internal fun prepareForContainerRemoval() {
        containerFullyAttached = false
        container?.setOnHierarchyChangeListener(null)
    }

    internal fun handleRequestedPermission(permission: String) = backstack
        .map { it.controller }
        .filter { it.didRequestPermission(permission) }
        .filter { it.shouldShowRequestPermissionRationale(permission) }
        .any()

    private fun popToTransaction(
        transaction: RouterTransaction,
        changeHandler: ControllerChangeHandler? = null
    ) {
        if (_backstack.size > 0) {
            val topTransaction = _backstack.peek()
            val updatedBackstack = backstack.dropLastWhile { it != transaction }
            setBackstack(updatedBackstack, changeHandler ?: topTransaction?.popChangeHandler)
        }
    }

    private fun performControllerChange(
        to: RouterTransaction?,
        from: RouterTransaction?,
        isPush: Boolean
    ) {
        if (isPush && to != null) {
            to.attachedToRouter = true
        }

        val changeHandler = when {
            isPush -> to?.pushChangeHandler
            from != null -> from.popChangeHandler
            else -> null
        }

        performControllerChange(to, from, isPush, changeHandler)
    }

    private fun performControllerChange(
        to: RouterTransaction?,
        from: RouterTransaction?,
        isPush: Boolean,
        changeHandler: ControllerChangeHandler?
    ) {
        var changeHandler = changeHandler

        val toController = to?.controller
        val fromController = from?.controller

        var forceDetachDestroy = false

        if (toController != null) {
            to.ensureValidIndex(transactionIndexer)
            setControllerRouter(toController)
        } else if (_backstack.size == 0 && !popsLastView) {
            // We're emptying out the backstack. Views get weird if you transition them out, so just no-op it. The host
            // Activity or controller should be handling this by finishing or at least hiding this view.
            changeHandler = NoOpControllerChangeHandler()
            forceDetachDestroy = true
        }

        performControllerChange(toController, fromController, isPush, changeHandler)

        val fromView = fromController?.view
        if (forceDetachDestroy && fromView != null) {
            fromController.detach(fromView, true, false, true, false)
        }
    }

    private fun performControllerChange(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        changeHandler: ControllerChangeHandler?
    ) {
        if (isPush && to != null && to.isDestroyed) {
            throw IllegalStateException("Trying to push a controller that has already been destroyed ${to.javaClass.simpleName}")
        }

        val transaction = ChangeTransaction(
            to,
            from,
            isPush,
            container,
            changeHandler,
            getAllChangeListeners(false)
        )

        if (pendingControllerChanges.size > 0) {
            // If we already have changes queued up (awaiting full container attach), queue this one up as well so they don't happen
            // out of order.
            pendingControllerChanges.add(transaction)
        } else if (container == null ||
                (from != null && (changeHandler == null
                        || changeHandler.removesFromViewOnPush) && !containerFullyAttached)
        ) {
            pendingControllerChanges.add(transaction)
            container?.post { performPendingControllerChanges() }
        } else {
            changeManager.executeChange(transaction)
        }
    }

    private fun performPendingControllerChanges() {
        if (container == null) {
            return
        }

        // We're intentionally using dynamic size checking (list.size()) here so we can account for changes
        // that occur during this loop (ex: if a controller is popped from within onAttach)
        pendingControllerChanges.indices
            .map { pendingControllerChanges[it] }
            .map {
                // we need to make sure that were using the current container
                // because it could be changed since we enqueued this change
                it.copy(container = container)
            }
            .forEach { changeManager.executeChange(it) }
        pendingControllerChanges.clear()
    }

    protected open fun pushToBackstack(entry: RouterTransaction) {
        if (_backstack.contains(entry.controller)) {
            throw IllegalStateException("Trying to push a controller that already exists on the backstack.")
        }
        _backstack.push(entry)
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

    private fun removeAllExceptVisibleAndUnowned() {
        val container = container ?: return

        val views = mutableListOf<View>()

        backstack
            .filterVisible()
            .mapNotNull { it.controller.view }
            .forEach { views.add(it) }

        siblingRouters
            .filter { it.container == container }
            .forEach { addRouterViewsToList(it, views) }

        (container.childCount - 1 downTo 0)
            .map { container.getChildAt(it) }
            .filterNot { views.contains(it) }
            .forEach { container.removeView(it) }
    }

    private fun addRouterViewsToList(router: Router, list: MutableList<View>) {
        router.backstack
            .map { it.controller }
            .forEach { controller ->
                controller.view?.let { list.add(it) }
                controller.childRouters.forEach { addRouterViewsToList(it, list) }
            }
    }

    private fun List<RouterTransaction>.filterVisible(): List<RouterTransaction> {
        val visible = mutableListOf<RouterTransaction>()

        for (transaction in this.reversed()) {
            visible.add(transaction)
            if (transaction.pushChangeHandler == null
                    || transaction.pushChangeHandler!!.removesFromViewOnPush
            ) {
                break
            }
        }

        return visible.reversed()
    }

    private fun backstacksAreEqual(
        lhs: List<RouterTransaction>,
        rhs: List<RouterTransaction>
    ): Boolean {
        if (lhs.size != rhs.size) return false

        lhs.forEachIndexed { i, transaction ->
            if (transaction != rhs[i]) return false
        }

        return true
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