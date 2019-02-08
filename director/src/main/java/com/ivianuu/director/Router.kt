package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.director.internal.DefaultControllerFactory
import com.ivianuu.director.internal.TransactionIndexer
import com.ivianuu.stdlibx.firstNotNullResultOrNull
import com.ivianuu.stdlibx.takeLastUntil

/**
 * Handles the backstack and delegates the host lifecycle to it's controllers
 */
open class Router internal constructor(
    containerId: Int,
    tag: String? = null,
    val host: Any,
    private val hostRouter: Router?
) {

    /**
     * The current backstack
     */
    val backstack: List<RouterTransaction> get() = _backstack
    private val _backstack = mutableListOf<RouterTransaction>()

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = false

    /**
     * The tag of this router
     */
    var tag: String? = tag
        private set

    /**
     * The container id of this router
     */
    var containerId: Int = containerId
        private set

    /**
     * The container of this router
     */
    var container: ViewGroup? = null
        private set

    var isBeingDestroyed: Boolean = false
        internal set(value) {
            _backstack.reversed().forEach { it.controller.isBeingDestroyed = true }
            field = value
        }

    /**
     * Will be used to instantiate controllers after config changes or process death
     */
    var controllerFactory: ControllerFactory?
        get() = _controllerFactory
        set(value) {
            _controllerFactory = value
                ?: DirectorPlugins.defaultControllerFactory
                        ?: DefaultControllerFactory
        }

    private var _controllerFactory: ControllerFactory =
        DirectorPlugins.defaultControllerFactory ?: DefaultControllerFactory

    internal val rootRouter: Router get() = hostRouter?.rootRouter ?: this

    private val transactionIndexer: TransactionIndexer by lazy(LazyThreadSafetyMode.NONE) {
        hostRouter?.transactionIndexer ?: TransactionIndexer()
    }

    private val changeListeners = mutableListOf<ChangeListenerEntry>()
    private val lifecycleListeners = mutableListOf<LifecycleListenerEntry>()

    private val changeManager = ControllerChangeManager()

    private var hasPreparedForContainerRemoval = false

    private var hostStarted = false
    private var hostDestroyed = false

    private val isRootRouter get() = hostRouter == null

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ControllerChangeHandler]
     */
    open fun setBackstack(
        newBackstack: List<RouterTransaction>,
        handler: ControllerChangeHandler? = null,
        isPush: Boolean? = null
    ) {
        val oldTransactions = _backstack.toList()
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

        check(newBackstack.size == newBackstack.distinctBy { it.controller }.size) {
            "Trying to push the same controller to the backstack more than once."
        }

        _backstack.clear()
        _backstack.addAll(newBackstack)

        val (destroyedVisibleTransactions, destroyedInvisibleTransactions) = oldTransactions
            // find destroyed transactions
            .filter { old -> newBackstack.none { it.controller == old.controller } }
            // Inform the controller that it will be destroyed soon
            .onEach { it.controller.isBeingDestroyed = true }
            .partition { it.controller.isAttached }

        // to ensure the destruction lifecycle onDetach -> onUnbindView -> onDestroy
        // we have to await until the view gets detached
        destroyedVisibleTransactions.forEach {
            it.controller.doOnPostDetach { _, _ ->
                it.controller.containerDetached()
                it.controller.hostDestroyed()
            }
        }

        // Ensure all new controllers have a valid router set
        newBackstack.forEach {
            it.attachedToRouter = true
            setControllerRouter(it.controller)
        }

        val newVisibleTransactions = newBackstack.filterVisible()

        val isSinglePush =
            newBackstack.isNotEmpty() && newBackstack.size - oldTransactions.size == 1
                    && newBackstack.dropLast(1) == oldTransactions

        val isSinglePop =
            !isSinglePush && oldTransactions.isNotEmpty() && oldTransactions.size - newBackstack.size == 1
                    && newBackstack == oldTransactions.dropLast(1)

        val isReplaceTop = !isSinglePush && !isSinglePop
                && newBackstack.isNotEmpty() && oldTransactions.isNotEmpty()
                && newBackstack.size == oldTransactions.size
                && newBackstack.dropLast(1) == oldTransactions.dropLast(1)
                && newBackstack.last() != oldTransactions.last()

        when {
            // just push the new top controller
            isSinglePush -> {
                performControllerChange(
                    newBackstack.last(),
                    oldTransactions.lastOrNull(),
                    true,
                    handler ?: newBackstack.last().pushChangeHandler
                )
            }
            // just pop the top controller
            isSinglePop -> {
                performControllerChange(
                    newBackstack.lastOrNull(),
                    oldTransactions.last(),
                    false,
                    handler ?: oldTransactions.last().popChangeHandler
                )
            }
            // just swap the top controllers
            isReplaceTop -> {
                val newTopTransaction = newBackstack.last()
                val oldTopTransaction = oldTransactions.last()

                val localHandler = handler ?: newTopTransaction.pushChangeHandler

                val oldHandlerRemovedViews =
                    oldTopTransaction.pushChangeHandler?.removesFromViewOnPush ?: true

                val newHandlerRemovesViews = localHandler?.removesFromViewOnPush ?: true

                if (oldHandlerRemovedViews && !newHandlerRemovesViews) {
                    newVisibleTransactions
                        .dropLast(1)
                        .forEachIndexed { i, transaction ->
                            performControllerChange(
                                transaction,
                                newVisibleTransactions.getOrNull(i - 1),
                                true, localHandler?.copy()
                            )
                        }
                } else if (!oldHandlerRemovedViews && newHandlerRemovesViews) {
                    oldVisibleTransactions
                        .dropLast(1)
                        .forEach {
                            val handler =
                                (localHandler?.copy() ?: SimpleSwapChangeHandler()).apply {
                                    forceRemoveViewOnPush = true
                                }
                            performControllerChange(null, it, true, handler)
                        }
                }

                // the view must be removed because its not the view we would expect
                // in a simple push transaction
                // the actual view below the new top is already pushed
                localHandler?.forceRemoveViewOnPush = true

                // swap the top controllers
                performControllerChange(
                    newTopTransaction,
                    oldTopTransaction,
                    true,
                    localHandler
                )
            }
            // it's not a simple change so loop trough everything
            newBackstack.isNotEmpty() -> {
                val newRootRequiresPush = isPush ?: newVisibleTransactions.isEmpty() ||
                        !oldTransactions.contains(newVisibleTransactions.first())

                if (newVisibleTransactions != oldVisibleTransactions) {
                    val oldRootTransaction = oldVisibleTransactions.firstOrNull()
                    val newRootTransaction = newVisibleTransactions.first()

                    // Replace the old visible root with the new one
                    if (oldRootTransaction == null || oldRootTransaction.controller != newRootTransaction.controller) {
                        // Ensure the existing root controller is fully pushed to the view hierarchy
                        if (oldRootTransaction != null) {
                            changeManager.cancelChange(
                                oldRootTransaction.controller.instanceId,
                                true
                            )
                        }

                        performControllerChange(
                            newRootTransaction,
                            oldRootTransaction,
                            newRootRequiresPush,
                            handler
                        )
                    }

                    // Remove all visible controllers that were previously on the backstack
                    oldVisibleTransactions
                        .drop(1)
                        .reversed()
                        .filterNot { newVisibleTransactions.contains(it) }
                        .forEach {
                            val localHandler = handler?.copy() ?: SimpleSwapChangeHandler()
                            localHandler.forceRemoveViewOnPush = true
                            changeManager.cancelChange(it.controller.instanceId, true)
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
            }
            // Remove all visible controllers that were previously on the backstack
            else -> {
                oldVisibleTransactions.reversed().forEach {
                    val localHandler = handler?.copy() ?: SimpleSwapChangeHandler()
                    changeManager.cancelChange(it.controller.instanceId, true)
                    performControllerChange(null, it, false, localHandler)
                }
            }
        }

        // destroy all invisible transactions here
        destroyedInvisibleTransactions.forEach {
            it.controller.containerDetached()
            it.controller.hostDestroyed()
        }
    }

    /**
     * This should be called by the host Activity when its onBackPressed method is called. The call will be forwarded
     * to its top [Controller]. If that controller doesn't handle it, then it will be popped.
     */
    fun handleBack(): Boolean {
        val currentTransaction = backstack.lastOrNull()

        return if (currentTransaction != null) {
            if (currentTransaction.controller.handleBack()) {
                true
            } else if (backstackSize > 0 && (popsLastView || backstackSize > 1)) {
                popCurrent()
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Attaches this Router's existing backstack to its container if one exists.
     */
    open fun rebind() {
        _backstack
            .filterVisible()
            .filterNot { it.controller.isAttached }
            .forEach {
                performControllerChange(
                    it, null, true,
                    SimpleSwapChangeHandler(false)
                )
            }
    }

    /**
     * Adds a listener for all controller change events
     */
    fun addChangeListener(listener: ControllerChangeListener, recursive: Boolean = false) {
        if (!getAllChangeListeners(false).contains(listener)) {
            changeListeners.add(ChangeListenerEntry(listener, recursive))
        }
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeChangeListener(listener: ControllerChangeListener) {
        changeListeners.removeAll { it.listener == listener }
    }

    internal open fun getAllChangeListeners(recursiveOnly: Boolean = false): List<ControllerChangeListener> {
        return changeListeners
            .filter { !recursiveOnly || it.recursive }
            .map { it.listener } + (hostRouter?.getAllChangeListeners(true) ?: emptyList())
    }

    /**
     * Adds a lifecycle listener for all controllers
     */
    fun addLifecycleListener(listener: ControllerLifecycleListener, recursive: Boolean = false) {
        if (!getAllLifecycleListeners(false).contains(listener)) {
            lifecycleListeners.add(LifecycleListenerEntry(listener, recursive))
        }
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeLifecycleListener(listener: ControllerLifecycleListener) {
        lifecycleListeners.removeAll { it.listener == listener }
    }

    internal fun getAllLifecycleListeners(recursiveOnly: Boolean = false): List<ControllerLifecycleListener> {
        return lifecycleListeners
            .filter { !recursiveOnly || it.recursive }
            .map { it.listener } + (hostRouter?.getAllLifecycleListeners(true) ?: emptyList())
    }

    fun setContainer(container: ViewGroup) {
        require(container.id == containerId) {
            "container id of the container must match the container id of this router"
        }

        if (this.container != container) {
            removeContainer()
            (container as? ControllerChangeListener)?.let { addChangeListener(it) }
            this.container = container

            _backstack.reversed().forEach { it.controller.containerAttached() }
        }
    }

    fun removeContainer() {
        if (container != null) {
            prepareForContainerRemoval()

            _backstack.reversed().forEach { it.controller.containerDetached() }

            (container as? ControllerChangeListener)?.let { removeChangeListener(it) }
            container = null
        }
    }

    fun hostStarted() {
        if (!hostStarted) {
            hostStarted = true
            hasPreparedForContainerRemoval = false
            _backstack.reversed().forEach { it.controller.hostStarted() }
        }
    }

    fun hostStopped() {
        if (hostStarted) {
            hostStarted = false
            prepareForContainerRemoval()
            _backstack.reversed().forEach { it.controller.hostStopped() }
        }
    }

    fun hostDestroyed() {
        if (!hostDestroyed) {
            hostDestroyed = true
            _backstack.reversed().forEach { it.controller.hostDestroyed() }
            container = null
        }
    }

    private fun prepareForContainerRemoval() {
        if (!hasPreparedForContainerRemoval) {
            hasPreparedForContainerRemoval = true
            _backstack.reversed().forEach {
                changeManager.cancelChange(it.controller.instanceId, true)
            }
        }
    }

    fun saveInstanceState(): Bundle {
        prepareForContainerRemoval()

        return Bundle().apply {
            val backstack = _backstack.map { it.saveInstanceState() }
            putParcelableArrayList(KEY_BACKSTACK, ArrayList(backstack))
            putInt(KEY_CONTAINER_ID, containerId)
            putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
            putString(KEY_TAG, tag)
            if (isRootRouter) {
                putBundle(
                    KEY_TRANSACTION_INDEXER,
                    transactionIndexer.saveInstanceState()
                )
            }
        }
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {
        val oldContainerId = containerId
        containerId = savedInstanceState.getInt(KEY_CONTAINER_ID)

        check(oldContainerId == containerId) {
            "Instance state does not belong to this router container id was $oldContainerId but is $containerId"
        }

        val oldTag = tag
        tag = savedInstanceState.getString(KEY_TAG)

        check(oldTag == tag) {
            "Instance state does not belong to this router tag was $oldTag but is $tag"
        }

        _backstack.clear()
        _backstack.addAll(
            savedInstanceState.getParcelableArrayList<Bundle>(KEY_BACKSTACK)!!
                .map { RouterTransaction.fromBundle(it, _controllerFactory) }
        )

        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        if (isRootRouter) {
            transactionIndexer.restoreInstanceState(
                savedInstanceState.getBundle(KEY_TRANSACTION_INDEXER)!!
            )
        }

        _backstack.reversed().forEach { setControllerRouter(it.controller) }
    }

    private fun performControllerChange(
        to: RouterTransaction?,
        from: RouterTransaction?,
        isPush: Boolean,
        handler: ControllerChangeHandler? = null
    ) {
        check(!isPush || to == null || to.controller.state != DESTROYED) {
            "Trying to push a controller that has already been destroyed ${to!!.javaClass.simpleName}"
        }

        val container = container ?: return

        val handler = handler ?: when {
            isPush -> to?.pushChangeHandler
            from != null -> from.popChangeHandler
            else -> null
        }

        changeManager.executeChange(
            to?.controller,
            from?.controller,
            isPush,
            container,
            handler,
            getAllChangeListeners(false)
        )
    }

    private fun setControllerRouter(controller: Controller) {
        if (!controller.routerSet) {
            controller.setRouter(this)

            if (hasContainer) {
                controller.containerAttached()
            }

            // bring them in the correct state
            if (hostStarted) {
                controller.hostStarted()
            }
        }
    }

    private fun List<RouterTransaction>.filterVisible(): List<RouterTransaction> =
        takeLastUntil {
            it.pushChangeHandler != null
                    && !it.pushChangeHandler.removesFromViewOnPush
        }

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
        private const val KEY_CONTAINER_ID = "Router.containerId"
        private const val KEY_POPS_LAST_VIEW = "Router.popsLastView"
        private const val KEY_TAG = "Router.tag"
        private const val KEY_TRANSACTION_INDEXER = "Router.transactionIndexer"
    }
}

/**
 * Returns a new router instance
 */
fun Router(
    containerId: Int,
    host: Any,
    savedInstanceState: Bundle? = null,
    tag: String? = null,
    hostRouter: Router? = null
): Router {
    val router = Router(containerId, tag, host, hostRouter)
    savedInstanceState?.let { router.restoreInstanceState(it) }
    return router
}

/**
 * Returns a new router instance
 */
fun Router(
    container: ViewGroup,
    host: Any,
    savedInstanceState: Bundle? = null,
    tag: String? = null,
    hostRouter: Router? = null
): Router = Router(container.id, host, savedInstanceState, tag, hostRouter).apply {
    setContainer(container)
    rebind()
}

/**
 * Whether or not the router has currently a container attached to it
 */
val Router.hasContainer: Boolean get() = container != null

/**
 * The current size of the backstack
 */
val Router.backstackSize: Int get() = backstack.size

/**
 * Whether or not this router has a root [Controller]
 */
val Router.hasRootController: Boolean get() = backstackSize > 0

/**
 * Pops the top [Controller] from the backstack
 */
fun Router.popCurrent(handler: ControllerChangeHandler? = null) {
    val transaction = backstack.lastOrNull()
        ?: error("Trying to pop the current controller when there are none on the backstack.")
    pop(transaction.controller, handler)
}

/**
 * Returns the hosted Controller with the given instance id or `null` if no such
 * Controller exists in this Router.
 */
fun Router.findControllerByInstanceId(instanceId: String): Controller? =
    backstack.firstNotNullResultOrNull {
        if (it.controller.instanceId == instanceId) {
            it.controller
        } else {
            it.controller.childRouters.firstNotNullResultOrNull {
                it.findControllerByInstanceId(instanceId)
            }
        }
    }

/**
 * Returns the hosted Controller that was pushed with the given tag or `null` if no
 * such Controller exists in this Router.
 */
fun Router.findControllerByTag(tag: String): Controller? =
    backstack.firstOrNull { it.tag == tag }?.controller

/**
 * Pops the passed [Controller] from the backstack
 */
fun Router.pop(
    controller: Controller,
    handler: ControllerChangeHandler? = null
) {
    val oldBackstack = backstack
    val newBackstack = oldBackstack.toMutableList()
    newBackstack.removeAll { it.controller == controller }

    val topTransaction = oldBackstack.lastOrNull()
    val poppingTopController = topTransaction?.controller == controller

    val handler = handler ?: if (poppingTopController) {
        topTransaction!!.popChangeHandler
    } else {
        null
    }

    setBackstack(newBackstack, handler)
}

/**
 * Pops the passed [transaction] from the backstack
 */
fun Router.pop(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val oldBackstack = backstack
    val newBackstack = oldBackstack.toMutableList()
    newBackstack.removeAll { it == transaction }

    val topTransaction = oldBackstack.lastOrNull()
    val poppingTop = topTransaction == transaction

    val handler = handler ?: if (poppingTop) {
        topTransaction!!.popChangeHandler
    } else {
        null
    }

    setBackstack(newBackstack, handler)
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.push(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(transaction)
    if (handler != null) {
        setBackstack(newBackstack, handler)
    } else {
        setBackstack(newBackstack)
    }
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.push(
    controller: Controller,
    pushHandler: ControllerChangeHandler? = DirectorPlugins.defaultPushHandler,
    popHandler: ControllerChangeHandler? = DirectorPlugins.defaultPopHandler,
    tag: String? = null,
    handler: ControllerChangeHandler? = null
): RouterTransaction = RouterTransaction(controller, pushHandler, popHandler, tag)
    .also { push(it, handler) }

/**
 * Replaces this Router's top [Controller] with the [Controller] of the [transaction]
 */
fun Router.replaceTop(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    val from = newBackstack.lastOrNull()
    if (from != null) {
        newBackstack.removeAt(newBackstack.lastIndex)
    }
    newBackstack.add(transaction)

    if (handler != null) {
        setBackstack(newBackstack, handler)
    } else {
        setBackstack(newBackstack)
    }
}

/**
 * Replaces this Router's top [Controller] with the [Controller]
 */
fun Router.replaceTop(
    controller: Controller,
    pushHandler: ControllerChangeHandler? = DirectorPlugins.defaultPushHandler,
    popHandler: ControllerChangeHandler? = DirectorPlugins.defaultPopHandler,
    tag: String? = null,
    handler: ControllerChangeHandler? = null
): RouterTransaction =
    RouterTransaction(controller, pushHandler, popHandler, tag).also { replaceTop(it, handler) }

/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(handler: ControllerChangeHandler? = null) {
    backstack.firstOrNull()
        ?.let { popTo(it, handler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, handler: ControllerChangeHandler? = null) {
    backstack.firstOrNull { it.tag == tag }
        ?.let { popTo(it, handler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popTo(
    controller: Controller,
    handler: ControllerChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { popTo(it, handler) }
}

/***
 * Pops all [Controller]s until the [transaction] is at the top
 */
fun Router.popTo(
    transaction: RouterTransaction,
    handler: ControllerChangeHandler? = null
) {
    if (backstack.isNotEmpty()) {
        val topTransaction = backstack.lastOrNull()
        val newBackstack = backstack.dropLastWhile { it != transaction }
        setBackstack(newBackstack, handler ?: topTransaction?.popChangeHandler)
    }
}

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun Router.setRoot(transaction: RouterTransaction, handler: ControllerChangeHandler? = null) {
    setBackstack(listOf(transaction), handler ?: transaction.pushChangeHandler)
}

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun Router.setRoot(
    controller: Controller,
    pushHandler: ControllerChangeHandler? = DirectorPlugins.defaultPushHandler,
    popHandler: ControllerChangeHandler? = DirectorPlugins.defaultPopHandler,
    tag: String? = null,
    handler: ControllerChangeHandler? = null
): RouterTransaction =
    RouterTransaction(controller, pushHandler, popHandler, tag).also { setRoot(it, handler) }