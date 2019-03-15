package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.director.internal.ListenersHolder
import com.ivianuu.stdlibx.firstNotNullResultOrNull
import com.ivianuu.stdlibx.takeLastUntil

/**
 * Handles the backstack and delegates the host lifecycle to it's [Controller]s
 */
class Router internal constructor(
    containerId: Int,
    tag: String? = null,
    val routerManager: RouterManager
) {

    /**
     * The current backstack
     */
    val backstack: List<Transaction> get() = _backstack
    private val _backstack = mutableListOf<Transaction>()

    /**
     * Whether or not the last view should be popped
     */
    var popsLastView = DirectorPlugins.defaultPopsLastView

    /**
     * Whether or not touch events should be blocked while changing controllers
     */
    var blockTouchesOnTransactions = DirectorPlugins.defaultBlockTouchesOnTransactions
        set(value) {
            field = value
            if (!value) container?.ignoreTouchEvents = false
        }

    /**
     * Whether or not back presses should be blocked while changing controllers
     */
    var blockBackClicksOnTransactions = DirectorPlugins.defaultBlockBackClicksOnTransactions

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
    var container: ControllerContainer? = null
        private set
    private var realContainer: ViewGroup? = null

    internal val listeners: ListenersHolder<RouterListener> =
        ListenersHolder(parent = hostRouter?.listeners)
    internal val controllerListeners: ListenersHolder<ControllerListener> =
        ListenersHolder(parent = hostRouter?.controllerListeners)

    private var hostStarted = false
    private var hostDestroyed = false

    private val hostRouter get() = (routerManager.host as? Controller)?.router

    private var inProgressTransactions = 0
        set(value) {
            field = value
            container?.ignoreTouchEvents = blockTouchesOnTransactions && value > 0
        }

    private val internalChangeListener = RouterListener(
        onChangeStarted = { _, _, _, _, _, _ -> inProgressTransactions++ },
        onChangeEnded = { _, _, _, _, _, _ -> inProgressTransactions-- }
    )

    val internalControllerListener = ControllerListener(
        postDetach = { controller, _ ->
            if (destroyingControllers.contains(controller)) {
                destroyingControllers.remove(controller)
                controller.containerRemoved()
                controller.hostDestroyed()
            }
        }
    )

    private val destroyingControllers = mutableListOf<Controller>()

    init {
        addListener(internalChangeListener)
    }

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ChangeHandler]
     */
    fun setBackstack(
        newBackstack: List<Transaction>,
        isPush: Boolean,
        handler: ChangeHandler? = null
    ) {
        if (newBackstack == _backstack) return

        // Swap around transaction indices to ensure they don't get thrown out of order by the
        // developer rearranging the backstack at runtime.
        val indices = newBackstack
            .onEach { it.ensureValidIndex(routerManager.transactionIndexer) }
            .map(Transaction::transactionIndex)
            .sorted()

        newBackstack.forEachIndexed { i, transaction ->
            transaction.transactionIndex = indices[i]
        }

        check(newBackstack.size == newBackstack.distinctBy(Transaction::controller).size) {
            "Trying to push the same controller to the backstack more than once."
        }

        val oldTransactions = _backstack.toList()
        val oldVisibleTransactions = oldTransactions.filterVisible()

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
        destroyingControllers.addAll(
            destroyedVisibleTransactions.reversed()
                .map(Transaction::controller)
        )

        // Ensure all new controllers have a valid router set
        newBackstack.forEach {
            it.attachedToRouter = true
            setControllerRouter(it.controller)
        }

        val newVisibleTransactions = newBackstack.filterVisible()

        if (oldVisibleTransactions != newVisibleTransactions) {
            val oldTopTransaction = oldVisibleTransactions.lastOrNull()
            val newTopTransaction = newVisibleTransactions.lastOrNull()

            // check if we should animate the top transactions
            val replacingTopTransactions = newTopTransaction != null && (oldTopTransaction == null
                    || oldTopTransaction.controller != newTopTransaction.controller)

            // Remove all visible controllers that were previously on the backstack
            oldVisibleTransactions
                .dropLast(if (replacingTopTransactions) 1 else 0)
                .reversed()
                .filterNot { o -> newVisibleTransactions.any { it.controller == o.controller } }
                .forEach { transaction ->
                    ControllerChangeManager.cancelChange(transaction.controller.instanceId)
                    val localHandler = handler?.copy() ?: transaction.popChangeHandler?.copy()
                    ?: DefaultChangeHandler()
                    performControllerChange(
                        transaction,
                        null,
                        isPush,
                        localHandler,
                        true
                    )
                }

            // Add any new controllers to the backstack
            newVisibleTransactions
                .dropLast(if (replacingTopTransactions) 1 else 0)
                .filterNot { n -> oldVisibleTransactions.any { it.controller == n.controller } }
                .forEachIndexed { i, transaction ->
                    val localHandler = handler?.copy() ?: transaction.pushChangeHandler
                    performControllerChange(
                        newVisibleTransactions.getOrNull(i - 1),
                        transaction,
                        true,
                        localHandler,
                        false
                    )
                }

            // Replace the old visible top with the new one
            if (replacingTopTransactions) {
                val localHandler = handler?.copy()
                    ?: (if (isPush) newTopTransaction?.pushChangeHandler?.copy()
                    else oldTopTransaction?.popChangeHandler?.copy())
                    ?: DefaultChangeHandler()

                performControllerChange(
                    oldTopTransaction,
                    newTopTransaction,
                    isPush,
                    localHandler,
                    !newVisibleTransactions.contains(oldTopTransaction)
                )
            }
        }

        // destroy all invisible transactions here
        destroyedInvisibleTransactions.reversed().forEach {
            it.controller.containerRemoved()
            it.controller.hostDestroyed()
        }
    }

    /**
     * Let the current controller handles back click or pops the top controller if possible
     * Returns whether or not the back click was handled
     */
    fun handleBack(): Boolean {
        // ignore back clicks while transacting
        if (blockBackClicksOnTransactions && inProgressTransactions > 0) return true

        val currentTransaction = backstack.lastOrNull()

        return if (currentTransaction != null) {
            if (currentTransaction.controller.handleBack()) {
                true
            } else if (hasRoot && (popsLastView || backstackSize > 1)) {
                popTop()
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Attaches this Routers [backstack] to its [container] if one exists.
     */
    fun rebind() {
        if (container == null) return

        _backstack
            .filterVisible()
            .filterNot { it.controller.isAttached }
            .forEach {
                performControllerChange(
                    null, it, true,
                    DefaultChangeHandler(false),
                    false
                )
            }
    }

    /**
     * Notifies the [listener] on controller changes
     */
    fun addListener(listener: RouterListener, recursive: Boolean = false) {
        listeners.add(listener, recursive)
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeListener(listener: RouterListener) {
        listeners.remove(listener)
    }

    /**
     * Adds a listener for all controllers
     */
    fun addControllerListener(listener: ControllerListener, recursive: Boolean = false) {
        controllerListeners.add(listener, recursive)
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeControllerListener(listener: ControllerListener) {
        controllerListeners.remove(listener)
    }

    /**
     * Sets the container of this router
     */
    fun setContainer(container: ViewGroup) {
        require(container.id == containerId) {
            "container id of the container must match the container id of this router"
        }

        if (this.realContainer != container) {
            removeContainer()

            this.realContainer = container
            this.container = if (container is ControllerContainer) {
                container
            } else {
                ControllerContainer(container.context)
                    .also {
                        container.addView(
                            it, ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
            }

            _backstack.forEach { it.controller.containerSet() }
        }
    }

    /**
     * Removes the current container if set
     */
    fun removeContainer() {
        container?.let { container ->
            prepareForContainerRemoval()

            _backstack.reversed().forEach {
                it.controller.containerRemoved()
            }

            if (realContainer != container) {
                realContainer!!.removeView(container)
            }
        }

        realContainer = null
        container = null
    }

    internal fun hostStarted() {
        if (!hostStarted) {
            hostStarted = true
            _backstack.forEach { it.controller.hostStarted() }
        }
    }

    internal fun hostStopped() {
        if (hostStarted) {
            hostStarted = false
            prepareForContainerRemoval()
            _backstack.reversed().forEach { it.controller.hostStopped() }
        }
    }

    internal fun hostIsBeingDestroyed() {
        _backstack.reversed().forEach { it.controller.isBeingDestroyed = true }
    }

    internal fun hostDestroyed() {
        if (!hostDestroyed) {
            hostDestroyed = true
            _backstack.reversed().forEach { it.controller.hostDestroyed() }
            removeContainer()
        }
    }

    private fun prepareForContainerRemoval() {
        _backstack.reversed().forEach {
            ControllerChangeManager.cancelChange(it.controller.instanceId)
        }
    }

    /**
     * Saves the state of this router
     */
    fun saveInstanceState(): Bundle {
        prepareForContainerRemoval()

        return Bundle().apply {
            val backstack = _backstack.map(Transaction::saveInstanceState)
            putParcelableArrayList(KEY_BACKSTACK, ArrayList(backstack))
            putInt(KEY_CONTAINER_ID, containerId)
            putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
            putBoolean(KEY_BLOCK_TOUCHES_ON_TRANSACTIONS, blockTouchesOnTransactions)
            putBoolean(KEY_BLOCK_BACK_CLICKS_ON_TRANSACTIONS, blockBackClicksOnTransactions)
            putString(KEY_TAG, tag)
        }
    }

    /**
     * Restores the previously saved state state
     */
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
                .map { Transaction.fromBundle(it, routerManager._controllerFactory) }
        )

        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        blockTouchesOnTransactions = savedInstanceState.getBoolean(
            KEY_BLOCK_TOUCHES_ON_TRANSACTIONS
        )
        blockBackClicksOnTransactions = savedInstanceState.getBoolean(
            KEY_BLOCK_BACK_CLICKS_ON_TRANSACTIONS
        )

        _backstack.forEach { setControllerRouter(it.controller) }
    }

    private fun performControllerChange(
        from: Transaction?,
        to: Transaction?,
        isPush: Boolean,
        handler: ChangeHandler? = null,
        forceRemoveFromViewOnPush: Boolean
    ) {
        check(!isPush || to == null || to.controller.state != DESTROYED) {
            "Trying to push a controller that has already been destroyed ${to!!.javaClass.simpleName}"
        }

        val container = container ?: return

        val handlerToUse = handler ?: when {
            isPush -> to?.pushChangeHandler
            from != null -> from.popChangeHandler
            else -> null
        }

        ControllerChangeManager.executeChange(
            this,
            from?.controller,
            to?.controller,
            isPush,
            container,
            handlerToUse,
            forceRemoveFromViewOnPush,
            listeners.get()
        )
    }

    private fun setControllerRouter(controller: Controller) {
        if (!controller.routerSet) {
            controller.setRouter(this)

            // bring them in the correct state
            if (hasContainer) {
                controller.containerSet()
            }

            if (hostStarted) {
                controller.hostStarted()
            }
        }
    }

    private fun List<Transaction>.filterVisible(): List<Transaction> =
        takeLastUntil {
            it.pushChangeHandler != null
                    && !it.pushChangeHandler!!.removesFromViewOnPush
        }

    companion object {
        private const val KEY_BACKSTACK = "Router.backstack"
        private const val KEY_CONTAINER_ID = "Router.containerId"
        private const val KEY_BLOCK_TOUCHES_ON_TRANSACTIONS = "Router.blockTouchesOnTransactions"
        private const val KEY_BLOCK_BACK_CLICKS_ON_TRANSACTIONS =
            "Router.blockBackClicksOnTransactions"
        private const val KEY_POPS_LAST_VIEW = "Router.popsLastView"
        private const val KEY_TAG = "Router.tag"

        fun fromBundle(
            bundle: Bundle,
            routerManager: RouterManager
        ): Router = Router(
            bundle.getInt(KEY_CONTAINER_ID),
            bundle.getString(KEY_TAG),
            routerManager
        )
    }
}

val Router.hasContainer: Boolean get() = container != null

val Router.backstackSize: Int get() = backstack.size

val Router.hasRoot: Boolean get() = backstackSize > 0

fun Router.getControllerByTagOrNull(tag: String): Controller? =
    backstack.firstNotNullResultOrNull {
        if (it.tag == tag) {
            it.controller
        } else {
            it.controller.childRouterManager
                .getControllerByTagOrNull(tag)
        }
    }

fun Router.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun Router.getControllerByInstanceIdOrNull(instanceId: String): Controller? =
    backstack.firstNotNullResultOrNull {
        if (it.controller.instanceId == instanceId) {
            it.controller
        } else {
            it.controller.childRouterManager
                .getControllerByInstanceIdOrNull(instanceId)
        }
    }

fun Router.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")

/**
 * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
 */
fun Router.setRoot(transaction: Transaction, handler: ChangeHandler? = null) {
    // todo check if we should always use isPush=true
    setBackstack(listOf(transaction), true, handler ?: transaction.pushChangeHandler)
}

/**
 * Pushes a new [Controller] to the backstack
 */
fun Router.push(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    newBackstack.add(transaction)
    setBackstack(newBackstack, true, handler)
}

/**
 * Replaces this Router's top [Controller] with the [Controller] of the [transaction]
 */
fun Router.replaceTop(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.toMutableList()
    val from = newBackstack.lastOrNull()
    if (from != null) {
        newBackstack.removeAt(newBackstack.lastIndex)
    }
    newBackstack.add(transaction)
    setBackstack(newBackstack, true, handler)
}

/**
 * Pops the passed [Controller] from the backstack
 */
fun Router.pop(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { pop(it, handler) }
}

/**
 * Pops the passed [transaction] from the backstack
 */
fun Router.pop(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val oldBackstack = backstack
    val newBackstack = oldBackstack.toMutableList()
    newBackstack.removeAll { it == transaction }
    setBackstack(newBackstack, false, handler)
}

/**
 * Pops the top [Controller] from the backstack
 */
fun Router.popTop(handler: ChangeHandler? = null) {
    val transaction = backstack.lastOrNull()
        ?: error("Trying to pop the current controller when there are none on the backstack.")
    pop(transaction, handler)
}

/**
 * Pops all [Controller] until only the root is left
 */
fun Router.popToRoot(handler: ChangeHandler? = null) {
    backstack.firstOrNull()?.let { popTo(it, handler) }
}

/**
 * Pops all [Controller]s until the [Controller] with the passed tag is at the top
 */
fun Router.popToTag(tag: String, handler: ChangeHandler? = null) {
    backstack.firstOrNull { it.tag == tag }
        ?.let { popTo(it, handler) }
}

/**
 * Pops all [Controller]s until the [controller] is at the top
 */
fun Router.popTo(
    controller: Controller,
    handler: ChangeHandler? = null
) {
    backstack.firstOrNull { it.controller == controller }
        ?.let { popTo(it, handler) }
}

/***
 * Pops all [Controller]s until the [transaction] is at the top
 */
fun Router.popTo(
    transaction: Transaction,
    handler: ChangeHandler? = null
) {
    val newBackstack = backstack.dropLastWhile { it != transaction }
    setBackstack(newBackstack, false, handler)
}