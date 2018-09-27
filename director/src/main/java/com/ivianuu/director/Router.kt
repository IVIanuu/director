package com.ivianuu.director

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.internal.Backstack
import com.ivianuu.director.internal.ChangeTransaction
import com.ivianuu.director.internal.NoOpControllerChangeHandler
import com.ivianuu.director.internal.TransactionIndexer
import com.ivianuu.director.internal.completeChangeImmediately
import com.ivianuu.director.internal.d
import com.ivianuu.director.internal.executeChange
import com.ivianuu.director.internal.requireMainThread
import java.util.*

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

    /**
     * Whether or not this router has root [Controller]
     */
    val hasRootController get() = _backstack.size > 0

    /**
     * Returns this Router's host Activity or `null` if it has either not yet been attached to
     * an Activity or if the Activity has been destroyed.
     */
    abstract val activity: Activity?

    private val changeListeners = mutableListOf<ChangeListenerEntry>()
    private val lifecycleListeners = mutableListOf<LifecycleListenerEntry>()
    private val pendingControllerChanges = mutableListOf<ChangeTransaction>()
    protected val destroyingControllers = mutableListOf<Controller>()

    var popsLastView = false

    private var containerFullyAttached = false
    internal var isActivityStopped = false

    internal var container: ViewGroup? = null
        set(value) {
            (field as? ControllerChangeListener)?.let { removeChangeListener(it) }
            (value as? ControllerChangeListener)?.let { addChangeListener(it) }
            field = value
        }

    val containerId: Int
        get() = container?.id ?: 0

    internal abstract val hasHost: Boolean
    internal abstract val siblingRouters: List<Router>
    internal abstract val rootRouter: Router
    internal abstract val transactionIndexer: TransactionIndexer

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

        d { "pop controller $controller" }

        val topTransaction = _backstack.peek()
        val poppingTopController =
            topTransaction != null && topTransaction.controller == controller

        if (poppingTopController) {
            d { "popping top controller" }
            trackDestroyingController(_backstack.pop().also { it.controller.destroy() })
            if (changeHandler != null) {
                performControllerChange(_backstack.peek(), topTransaction, false, changeHandler)
            } else {
                performControllerChange(_backstack.peek(), topTransaction, false)
            }
        } else {
            d { "not popping top controller" }

            var removedTransaction: RouterTransaction? = null
            var nextTransaction: RouterTransaction? = null

            val backstack = _backstack.reversedEntries

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

            d { "next transaction attach $needsNextTransactionAttach" }
            d { "removed transaction ${removedTransaction?.controller}" }
            d { "next transaction ${nextTransaction?.controller}" }

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

            val newHandlerRemovesViews = handler == null || handler.removesFromViewOnPush

            if (!oldHandlerRemovedViews && newHandlerRemovesViews) {
                _backstack.reversedEntries
                    .filterVisible()
                    .forEach {
                        performControllerChange(null, it, true, handler)
                    }
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

        trackDestroyingControllers(poppedControllers)

        if (popViews && poppedControllers.isNotEmpty()) {
            val topTransaction = poppedControllers.first()
            topTransaction.controller.addLifecycleListener(object : ControllerLifecycleListener {
                override fun onChangeEnd(
                    controller: Controller,
                    changeHandler: ControllerChangeHandler,
                    changeType: ControllerChangeType
                ) {
                    if (changeType == ControllerChangeType.POP_EXIT) {
                        (poppedControllers.lastIndex downTo 1)
                            .map { poppedControllers[it] }
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
    fun findControllerByInstanceId(instanceId: String) = _backstack.entries
        .firstOrNull { it.controller.instanceId == instanceId }
        ?.controller

    /**
     * Returns the hosted Controller that was pushed with the given tag or `null` if no
     * such Controller exists in this Router.
     */
    fun findControllerByTag(tag: String) =
        _backstack.entries.firstOrNull { it.tag == tag }?.controller

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
        val transaction = _backstack.entries.firstOrNull { it.tag == tag } ?: return false
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
            _backstack.entries.firstOrNull { it.controller == controller } ?: return false
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

        d { "set backstack -> $newBackstack" }

        val oldTransactions = _backstack.entries

        d { "old transactions $oldTransactions" }

        val oldVisibleTransactions = _backstack.reversedEntries.filterVisible()

        d { "old visible transactions $oldVisibleTransactions" }

        removeAllExceptVisibleAndUnowned()
        ensureOrderedTransactionIndices(newBackstack)
        ensureNoDuplicateControllers(newBackstack)

        _backstack.setEntries(newBackstack)

        val transactionsToBeRemoved = oldTransactions
            .filter { old -> newBackstack.none { it.controller == old.controller } }
            .onEach {
                // Inform the controller that it will be destroyed soon
                it.controller.isBeingDestroyed = true
            }

        d { "transactions to be removed $transactionsToBeRemoved" }

        // Ensure all new controllers have a valid router set
        _backstack.entries.forEach {
            it.attachedToRouter = true
            setControllerRouter(it.controller)
        }

        if (newBackstack.isNotEmpty()) {
            d { "new backstack is not empty" }

            val newVisibleTransactions = newBackstack.reversed().filterVisible()

            d { "new visible transactions $newVisibleTransactions" }

            val newRootRequiresPush =
                newVisibleTransactions.isEmpty() ||
                        !oldTransactions.contains(newVisibleTransactions.first())

            d { "new visible is empty -> ${newVisibleTransactions.isEmpty()}" }
            d { "old transaction containts new ${oldTransactions.contains(newVisibleTransactions.first())}" }

            d { "new root requires push $newRootRequiresPush" }

            val visibleTransactionsChanged =
                !backstacksAreEqual(newVisibleTransactions, oldVisibleTransactions)

            d { "visible transactions changed $visibleTransactionsChanged" }

            if (visibleTransactionsChanged) {
                val oldRootTransaction = oldVisibleTransactions.firstOrNull()
                val newRootTransaction = newVisibleTransactions.first()

                d { "old root transaction $oldRootTransaction, new root transaction $newRootTransaction" }

                // Replace the old root with the new one
                if (oldRootTransaction == null || oldRootTransaction.controller != newRootTransaction.controller) {
                    d { "set new root" }

                    // Ensure the existing root controller is fully pushed to the view hierarchy
                    if (oldRootTransaction != null) {
                        completeChangeImmediately(oldRootTransaction.controller.instanceId)
                    }

                    performControllerChange(
                        newRootTransaction,
                        oldRootTransaction,
                        newRootRequiresPush,
                        changeHandler
                    )
                }

                oldVisibleTransactions
                    .drop(1)
                    .reversed()
                    .filterNot { newVisibleTransactions.contains(it) }
                    .forEach {
                        d { "remove visible controller old -> $it" }
                        val localHandler = changeHandler?.copy() ?: SimpleSwapChangeHandler()
                        localHandler.forceRemoveViewOnPush = true
                        completeChangeImmediately(it.controller.instanceId)
                        performControllerChange(
                            null,
                            it,
                            newRootRequiresPush,
                            localHandler
                        )
                    }

                var lastVisibleTransaction: RouterTransaction? = null
                // Add any new controllers to the backstack
                newVisibleTransactions
                    .drop(1)
                    .filterNot { oldVisibleTransactions.contains(it) }
                    .forEach {
                        d { "add new controller tp backstack -> $it" }

                        performControllerChange(
                            it,
                            lastVisibleTransaction,
                            true,
                            it.pushChangeHandler
                        )

                        lastVisibleTransaction = it
                    }
            }
        } else {
            // Remove all visible controllers that were previously on the backstack
            /**oldVisibleTransactions.reversed().forEach {
            d { "remove visible controller new -> $it" }
            val localHandler = changeHandler?.copy() ?: SimpleSwapChangeHandler()
            completeChangeImmediately(it.controller.instanceId)
            performControllerChange(null, it, false, localHandler)
            }*/

            for (i in oldVisibleTransactions.size - 1 downTo 0) {
                d { "remove visible controller old -> ${oldVisibleTransactions[i]}" }
                val transaction = oldVisibleTransactions[i]
                val localHandler = changeHandler?.copy() ?: SimpleSwapChangeHandler()
                completeChangeImmediately(transaction.controller.instanceId)
                performControllerChange(null, transaction, false, localHandler)
            }
        }

        // Destroy all old controllers that are no longer on the backstack. We don't do this when we initially
        // set the backstack to prevent the possibility that they'll be destroyed before the controller
        // change handler runs.
        d { "destroy removed transactions $transactionsToBeRemoved" }
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
            d { "actual add listener $listener, $recursive" }
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
            .also { d { "get all listeners $recursiveOnly, listeners $lifecycleListeners" } }
            .filter { !recursiveOnly || it.recursive }
            .also { d { "get all listeners $recursiveOnly, filtered $it" } }
            .map { it.listener }

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

        d { "rebind if needed" }

        _backstack.entries
            .filter { it.controller.needsAttach.apply {
                d { "needs attach ? $it ${it.controller.needsAttach}" }
            } }
            .forEach { performControllerChange(it, null, true,
                SimpleSwapChangeHandler(false)
            ) }
    }

    fun onActivityStarted(activity: Activity) {
        isActivityStopped = false

        _backstack.reversedEntries.forEach { transaction ->
            transaction.controller.activityStarted(activity)
            transaction.controller.childRouters.forEach { it.onActivityStarted(activity) }
        }
    }

    fun onActivityResumed(activity: Activity) {
        _backstack.reversedEntries.forEach { transaction ->
            transaction.controller.activityResumed(activity)
            transaction.controller.childRouters.forEach { it.onActivityResumed(activity) }
        }
    }

    fun onActivityPaused(activity: Activity) {
        _backstack.reversedEntries.forEach { transaction ->
            transaction.controller.activityPaused(activity)
            transaction.controller.childRouters.forEach { it.onActivityPaused(activity) }
        }
    }

    fun onActivityStopped(activity: Activity) {
        _backstack.reversedEntries.forEach { transaction ->
            transaction.controller.activityStopped(activity)
            transaction.controller.childRouters.forEach { it.onActivityStopped(activity) }
        }

        isActivityStopped = true
    }

    open fun onActivityDestroyed(activity: Activity) {
        prepareForContainerRemoval()
        // todo changeListeners.clear()
        // todo lifecycleListeners.clear()

        _backstack.reversedEntries.forEach { transaction ->
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
        _backstack.reversedEntries.forEach {
            if (completeChangeImmediately(it.controller.instanceId)) {
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

        _backstack.restoreInstanceState(backstackBundle)
        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        _backstack.entries.forEach { setControllerRouter(it.controller) }
    }

    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        backstack
            .map { it.controller }
            .forEach { controller ->
                controller.createOptionsMenu(menu, inflater)
                controller.childRouters.forEach { it.onCreateOptionsMenu(menu, inflater) }
            }
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        backstack
            .map { it.controller }
            .forEach { controller ->
                controller.prepareOptionsMenu(menu)
                controller.childRouters.forEach { it.onPrepareOptionsMenu(menu) }
            }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return backstack
            .map { it.controller }
            .any { controller ->
                controller.optionsItemSelected(item)
                        || controller.childRouters.any {
                    it.onOptionsItemSelected(item)
                }
            }
    }

    private fun popToTransaction(
        transaction: RouterTransaction,
        changeHandler: ControllerChangeHandler? = null
    ) {
        if (_backstack.size > 0) {
            val topTransaction = _backstack.peek()

            val updatedBackstack = mutableListOf<RouterTransaction>()
            val backstackIterator = _backstack.entries

            for (existingTransaction in backstackIterator) {
                updatedBackstack.add(existingTransaction)
                if (existingTransaction == transaction) {
                    break
                }
            }

            setBackstack(updatedBackstack, changeHandler ?: topTransaction?.popChangeHandler)
        }
    }

    internal fun watchContainerAttach() {
        container?.post { containerFullyAttached = true }
    }

    internal fun prepareForContainerRemoval() {
        containerFullyAttached = false
        container?.setOnHierarchyChangeListener(null)
    }

    internal open fun onContextAvailable() {
        _backstack.reversedEntries.forEach { it.controller.contextAvailable() }
    }

    fun handleRequestedPermission(permission: String) = backstack
        .map { it.controller }
        .filter { it.didRequestPermission(permission) }
        .filter { it.shouldShowRequestPermissionRationale(permission) }
        .any()

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

        d { "change handler is $changeHandler" }

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

        d { "perform controller change to $toController, from $from, force detach $forceDetachDestroy" }

        performControllerChange(toController, fromController, isPush, changeHandler)

        val fromView = fromController?.view
        if (forceDetachDestroy && fromView != null) {
            fromController.detach(fromView, true, false, true)
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
        } else if (from != null && (changeHandler == null || changeHandler.removesFromViewOnPush) && !containerFullyAttached) {
            // If the change handler will remove the from view, we have to make sure the container is fully attached first so we avoid NPEs
            // within ViewGroup (details on issue #287). Post this to the container to ensure the attach is complete before we try to remove
            // anything.
            pendingControllerChanges.add(transaction)
            container!!.post { performPendingControllerChanges() }
        } else {
            executeChange(transaction)
        }
    }

    private fun performPendingControllerChanges() {
        // We're intentionally using dynamic size checking (list.size()) here so we can account for changes
        // that occur during this loop (ex: if a controller is popped from within onAttach)
        pendingControllerChanges.indices
            .map { pendingControllerChanges[it] }
            .forEach { executeChange(it) }
        pendingControllerChanges.clear()
    }

    protected open fun pushToBackstack(entry: RouterTransaction) {
        if (_backstack.contains(entry.controller)) {
            throw IllegalStateException("Trying to push a controller that already exists on the backstack.")
        }
        _backstack.push(entry)
    }

    protected open fun setControllerRouter(controller: Controller) {
        controller.router = this
        controller.contextAvailable()
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

    private fun trackDestroyingControllers(transactions: List<RouterTransaction>) {
        transactions.forEach { trackDestroyingController(it) }
    }

    private fun removeAllExceptVisibleAndUnowned() {

        val views = mutableListOf<View>()

        _backstack.reversedEntries
            .filterVisible()
            .mapNotNull { it.controller.view }
            .forEach { views.add(it) }

        siblingRouters
            .filter { it.container == container }
            .forEach { addRouterViewsToList(it, views) }


        val container = container ?: return
        (container.childCount -1 downTo 0)
            .map { container.getChildAt(it) }
            .filterNot { views.contains(it) }
            .forEach { container.removeView(it) }
    }

    // Swap around transaction indices to ensure they don't get thrown out of order by the
    // developer rearranging the backstack at runtime.
    private fun ensureOrderedTransactionIndices(backstack: List<RouterTransaction>) {
        val indices = ArrayList<Int>(backstack.size)
        for (transaction in backstack) {
            transaction.ensureValidIndex(transactionIndexer)
            indices.add(transaction.transactionIndex)
        }

        indices.sort()

        backstack.indices.forEach { backstack[it].transactionIndex = indices[it] }
    }

    private fun ensureNoDuplicateControllers(backstack: List<RouterTransaction>) {
        if (backstack.size != backstack.distinctBy { it.controller }.size) {
            throw IllegalStateException("Trying to push the same controller to the backstack more than once.")
        }
    }

    private fun addRouterViewsToList(router: Router, list: MutableList<View>) {
        router.backstack.map { it.controller }.forEach { controller ->
            controller.view?.let { list.add(it) }
            controller.childRouters.forEach { addRouterViewsToList(it, list) }
        }
    }

    private fun List<RouterTransaction>.filterVisible(): List<RouterTransaction> {
        val visible = mutableListOf<RouterTransaction>()

        for (transaction in this) {
            visible.add(transaction)
            //noinspection ConstantConditions
            if (transaction.pushChangeHandler == null || transaction.pushChangeHandler!!.removesFromViewOnPush) {
                break
            }
        }

        return visible
    }

    private fun backstacksAreEqual(
        lhs: List<RouterTransaction>,
        rhs: List<RouterTransaction>
    ): Boolean {
        if (lhs.size != rhs.size) {
            return false
        }

        for (i in rhs.indices) {
            if (rhs[i].controller != lhs[i].controller) {
                return false
            }
        }

        return true
    }

    internal abstract fun invalidateOptionsMenu()
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