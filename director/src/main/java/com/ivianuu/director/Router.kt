package com.ivianuu.director

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.ControllerState.ATTACHED
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.director.internal.DefaultControllerFactory
import com.ivianuu.director.internal.TransactionIndexer
import com.ivianuu.stdlibx.safeAs
import com.ivianuu.stdlibx.takeLastUntil

/**
 * Handles the backstack and delegates the host lifecycle to it's controllers
 */
open class Router internal constructor(
    containerId: Int,
    tag: String? = null,
    val host: Any,
    val hostRouter: Router?
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
            _backstack.forEach { it.controller.isBeingDestroyed = true }
            field = value
        }

    /**
     * Will be used to instantiate controllers after config changes or process death
     */
    var controllerFactory: ControllerFactory?
        get() = _controllerFactory
        set(value) {
            _controllerFactory = value ?: DefaultControllerFactory()
        }

    private var _controllerFactory: ControllerFactory = DefaultControllerFactory()

    internal val rootRouter: Router get() = hostRouter?.rootRouter ?: this

    private val transactionIndexer: TransactionIndexer by lazy(LazyThreadSafetyMode.NONE) {
        hostRouter?.transactionIndexer ?: TransactionIndexer()
    }

    private val changeListeners = mutableListOf<ChangeListenerEntry>()
    private val lifecycleListeners = mutableListOf<LifecycleListenerEntry>()

    private val changeManager = ControllerChangeManager()

    private var hasPreparedForHostDetach = false

    private var hostStarted = false
        private set

    private val isRootRouter get() = hostRouter == null

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ControllerChangeHandler]
     */
    open fun setBackstack(
        newBackstack: List<RouterTransaction>,
        changeHandler: ControllerChangeHandler? = null
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
            .partition { it.controller.state == ATTACHED }

        // to ensure the destruction lifecycle onDetach -> onUnbindView -> onDestroy
        // we have to await until the view gets detached
        destroyedVisibleTransactions.forEach {
            val view = it.controller.view!!
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}
                override fun onViewDetachedFromWindow(v: View) {
                    view.removeOnAttachStateChangeListener(this)
                    it.controller.containerDetached()
                    it.controller.hostDestroyed()
                }
            })
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
                && newBackstack.size == oldTransactions.size
                && newBackstack.dropLast(1) == oldTransactions.dropLast(1)
                && newBackstack.lastOrNull() != oldTransactions.lastOrNull()

        when {
            // just push the new top controller
            isSinglePush -> {
                performControllerChange(
                    newBackstack.last(),
                    oldTransactions.lastOrNull(),
                    true,
                    changeHandler ?: newBackstack.last().pushChangeHandler
                )
            }
            // just pop the top controller
            isSinglePop -> {
                performControllerChange(
                    newBackstack.lastOrNull(),
                    oldTransactions.last(),
                    false,
                    changeHandler ?: oldTransactions.last().popChangeHandler
                )
            }
            // just swap the top controllers
            isReplaceTop -> {
                val newTopTransaction = newBackstack.last()
                val oldTopTransaction = oldTransactions.last()

                val oldHandlerRemovedViews =
                    oldTopTransaction.pushChangeHandler?.removesFromViewOnPush == true

                val localHandler = changeHandler ?: newTopTransaction.pushChangeHandler

                val newHandlerRemovesViews = localHandler?.removesFromViewOnPush == true

                if (oldHandlerRemovedViews && !newHandlerRemovesViews) {
                    // re attach old views which will be visible now except the top one
                    newVisibleTransactions
                        .dropLast(1)
                        .forEachIndexed { i, transaction ->
                            performControllerChange(
                                transaction,
                                newVisibleTransactions.getOrNull(i), true, localHandler!!.copy()
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
                    newTopTransaction.also {
                        it.allowModification = true
                        it.pushChangeHandler = localHandler
                        it.allowModification = false
                    },
                    oldTopTransaction,
                    true
                )
            }
            // it's not a simple change so loop trough everything
            newBackstack.isNotEmpty() -> {
                val newRootRequiresPush =
                    newVisibleTransactions.isEmpty() ||
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
                    val localHandler = changeHandler?.copy() ?: SimpleSwapChangeHandler()
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
     * Attaches this Router's existing backstack to its container if one exists.
     */
    open fun rebind() {
        _backstack
            .filterVisible()
            .filterNot { it.controller.state == ATTACHED }
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
            "container id must be matching the container id"
        }

        if (this.container != container) {
            removeContainer()
            (container as? ControllerChangeListener)?.let { addChangeListener(it) }
            this.container = container

            _backstack.forEach { it.controller.containerAttached() }
        }
    }

    fun removeContainer() {
        if (container != null) {
            prepareForContainerRemoval()

            _backstack.forEach { it.controller.containerDetached() }

            container?.let { container ->
                (container as? ControllerChangeListener)?.let { removeChangeListener(it) }
            }
            container = null
        }
    }

    fun hostStarted() {
        hostStarted = true
        hasPreparedForHostDetach = false
        _backstack.reversed().forEach { it.controller.hostStarted() }
    }

    fun hostStopped() {
        hostStarted = false
        if (!hasPreparedForHostDetach) {
            hasPreparedForHostDetach = true
            prepareForContainerRemoval()
        }
        _backstack.reversed().forEach { it.controller.hostStopped() }
    }

    fun hostDestroyed() {
        _backstack.reversed().forEach { it.controller.hostDestroyed() }
        container = null
    }

    private fun prepareForContainerRemoval() {
        _backstack.reversed().forEach {
            changeManager.cancelChange(it.controller.instanceId, true)
        }
    }

    fun saveInstanceState(): Bundle {
        if (!hasPreparedForHostDetach) {
            hasPreparedForHostDetach = true
            prepareForContainerRemoval()
        }

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
        _backstack.clear()
        _backstack.addAll(
            savedInstanceState.getParcelableArrayList<Bundle>(KEY_BACKSTACK)!!
                .map { RouterTransaction.fromBundle(it, _controllerFactory) }
        )

        containerId = savedInstanceState.getInt(KEY_CONTAINER_ID)

        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        tag = savedInstanceState.getString(KEY_TAG)

        _backstack.forEach { setControllerRouter(it.controller) }

        if (isRootRouter) {
            transactionIndexer.restoreInstanceState(
                savedInstanceState.getBundle(KEY_TRANSACTION_INDEXER)!!
            )
        }
    }

    private fun performControllerChange(
        to: RouterTransaction?,
        from: RouterTransaction?,
        isPush: Boolean,
        changeHandler: ControllerChangeHandler? = null
    ) {
        check(!isPush || to == null || to.controller.state != DESTROYED) {
            "Trying to push a controller that has already been destroyed ${to!!.javaClass.simpleName}"
        }

        val container = container ?: return

        val changeHandler = changeHandler ?: when {
            isPush -> to?.pushChangeHandler
            from != null -> from.popChangeHandler
            else -> null
        }

        changeManager.executeChange(
            to?.controller,
            from?.controller,
            isPush,
            container,
            changeHandler,
            getAllChangeListeners(false)
        )
    }

    private fun setControllerRouter(controller: Controller) {
        if (!controller.routerSet) {
            // make sure to set the parent controller before the
            // router is set
            host.safeAs<Controller>()?.let { controller.parentController = it }

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
                    && !it.pushChangeHandler!!.removesFromViewOnPush
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
 * Returns a new [Router] instance
 */
fun Router(
    host: Any,
    containerId: Int,
    tag: String? = null,
    hostRouter: Router? = null,
    savedInstanceState: Bundle? = null,
    controllerFactory: ControllerFactory? = null
): Router = Router(containerId, tag, host, hostRouter).apply {
    controllerFactory?.let { this.controllerFactory = it }
    savedInstanceState?.let { restoreInstanceState(it) }
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
 * Fluent version of pops last view
 */
fun Router.popsLastView(popsLastView: Boolean): Router = apply { this.popsLastView = popsLastView }

/**
 * Saves the instance state of [controller] which can later be used in
 * [Controller.setInitialSavedState]
 */
fun Router.saveControllerInstanceState(controller: Controller): Bundle {
    require(backstack.any { it.controller == controller }) {
        "controller is not attached to the router"
    }

    return controller.saveInstanceState()
}

/**
 * Returns the hosted Controller with the given instance id or `null` if no such
 * Controller exists in this Router.
 */
fun Router.findControllerByInstanceId(instanceId: String): Controller? {
    return backstack
        .mapNotNull { it.controller.findController(instanceId) }
        .firstOrNull()
}

/**
 * Returns the hosted Controller that was pushed with the given tag or `null` if no
 * such Controller exists in this Router.
 */
fun Router.findControllerByTag(tag: String): Controller? =
    backstack.firstOrNull { it.tag == tag }?.controller

/**
 * This should be called by the host Activity when its onBackPressed method is called. The call will be forwarded
 * to its top [Controller]. If that controller doesn't handle it, then it will be popped.
 */
fun Router.handleBack(): Boolean {
    val currentTransaction = backstack.lastOrNull()

    return if (currentTransaction != null) {
        if (currentTransaction.controller.handleBack()) {
            true
        } else if (backstackSize > 0 && (popsLastView || backstackSize > 1)) {
            popCurrentController()
            true
        } else {
            false
        }
    } else {
        false
    }
}

/**
 * Pops the top [Controller] from the backstack
 */
fun Router.popCurrentController(changeHandler: ControllerChangeHandler? = null) {
    val transaction = backstack.lastOrNull()
        ?: error("Trying to pop the current controller when there are none on the backstack.")
    popController(transaction.controller, changeHandler)
}

/**
 * Pops the passed [Controller] from the backstack
 */
fun Router.popController(
    controller: Controller,
    changeHandler: ControllerChangeHandler? = null
) {
    val oldBackstack = backstack
    val newBackstack = oldBackstack.toMutableList()
    newBackstack.removeAll { it.controller == controller }

    val topTransaction = oldBackstack.lastOrNull()
    val poppingTopController = topTransaction?.controller == controller

    val changeHandler = changeHandler ?: if (poppingTopController) {
        topTransaction!!.popChangeHandler
    } else {
        null
    }

    setBackstack(newBackstack, changeHandler)
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.pushController(
    transaction: RouterTransaction,
    changeHandler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(transaction)
    if (changeHandler != null) {
        setBackstack(newBackstack, changeHandler)
    } else {
        setBackstack(newBackstack)
    }
}

/**
 * Replaces this Router's top [Controller] with the [Controller] of the [transaction]
 */
fun Router.replaceTopController(
    transaction: RouterTransaction,
    changeHandler: ControllerChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    val from = newBackstack.lastOrNull()
    if (from != null) {
        newBackstack.removeAt(newBackstack.lastIndex)
    }
    newBackstack.add(transaction)

    if (changeHandler != null) {
        setBackstack(newBackstack, changeHandler)
    } else {
        setBackstack(newBackstack)
    }
}


/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(changeHandler: ControllerChangeHandler? = null) {
    backstack.firstOrNull()
        ?.let { popToTransaction(it, changeHandler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, changeHandler: ControllerChangeHandler? = null) {
    backstack.firstOrNull { it.tag == tag }
        ?.let { popToTransaction(it, changeHandler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popToController(
    controller: Controller,
    changeHandler: ControllerChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { popToTransaction(it, changeHandler) }
}

/***
 * Pops all [Controller]s until the [transaction] is at the top
 */
fun Router.popToTransaction(
    transaction: RouterTransaction,
    changeHandler: ControllerChangeHandler? = null
) {
    if (backstack.isNotEmpty()) {
        val topTransaction = backstack.lastOrNull()
        val newBackstack = backstack.dropLastWhile { it != transaction }
        setBackstack(newBackstack, changeHandler ?: topTransaction?.popChangeHandler)
    }
}

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun Router.setRoot(transaction: RouterTransaction, changeHandler: ControllerChangeHandler? = null) {
    setBackstack(listOf(transaction), changeHandler ?: transaction.pushChangeHandler)
}