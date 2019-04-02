package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.stdlibx.firstNotNullResultOrNull
import com.ivianuu.stdlibx.safeAs
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

    private val listeners =
        mutableListOf<ListenerEntry<RouterListener>>()
    private val controllerListeners =
        mutableListOf<ListenerEntry<ControllerListener>>()

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

        // find destroyed transactions
        val destroyedTransactions = oldTransactions
            .filter { old -> newBackstack.none { it.controller == old.controller } }

        // Inform the controller that it will be destroyed soon
        destroyedTransactions.forEach {
            it.controller.isBeingDestroyed = true
        }

        val destroyedInvisibleTransactions = destroyedTransactions
            .filterNot { it.controller.isAttached }

        // Ensure all new controllers have a valid router set
        newBackstack.forEach {
            it.attachedToRouter = true
            moveControllerToCorrectState(it.controller)
        }

        val newVisibleTransactions = newBackstack.filterVisible()

        if (oldVisibleTransactions != newVisibleTransactions) {
            val oldTopTransaction = oldVisibleTransactions.lastOrNull()
            val newTopTransaction = newVisibleTransactions.lastOrNull()

            // check if we should animate the top transactions
            val replacingTopTransactions = newTopTransaction != null && (oldTopTransaction == null
                    || oldTopTransaction.controller != newTopTransaction.controller)

            // Remove all visible controllers that were previously on the backstack
            // from top to bottom
            oldVisibleTransactions
                .dropLast(if (replacingTopTransactions) 1 else 0)
                .reversed()
                .filterNot { old -> newVisibleTransactions.any { it.controller == old.controller } }
                .forEach { transaction ->
                    ControllerChangeManager.cancelChange(transaction.controller.instanceId)
                    val localHandler = handler?.copy()
                        ?: transaction.popChangeHandler?.copy()
                        ?: DefaultChangeHandler()
                    performControllerChange(
                        transaction,
                        null,
                        isPush,
                        localHandler,
                        true
                    )
                }

            // Add any new controllers to the backstack from bottom to top
            newVisibleTransactions
                .dropLast(if (replacingTopTransactions) 1 else 0)
                .filterNot { new -> oldVisibleTransactions.any { it.controller == new.controller } }
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

                val forceRemoveFromView = if (oldTopTransaction != null) {
                    !newVisibleTransactions.contains(oldTopTransaction)
                } else {
                    false
                }

                performControllerChange(
                    oldTopTransaction,
                    newTopTransaction,
                    isPush,
                    localHandler,
                    forceRemoveFromView
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
        listeners.add(ListenerEntry(listener, recursive))
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeListener(listener: RouterListener) {
        listeners.removeAll { it.listener == listener }
    }

    /**
     * Adds the [listener] to all controllers
     */
    fun addControllerListener(listener: ControllerListener, recursive: Boolean = false) {
        controllerListeners.add(ListenerEntry(listener, recursive))
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeControllerListener(listener: ControllerListener) {
        controllerListeners.removeAll { it.listener == listener }
    }

    /**
     * Sets the container of this router
     */
    fun setContainer(container: ViewGroup) {
        require(container.id == containerId) {
            "container id of the container must match the container id of this router"
        }

        if (this.container != container) {
            removeContainer()
            this.container = container
            _backstack.forEach { it.controller.containerSet() }
        }
    }

    /**
     * Removes the current container if set
     */
    fun removeContainer() {
        if (container == null) return
        prepareForContainerRemoval()
        _backstack.reversed().forEach { it.controller.containerRemoved() }
        container = null
    }

    internal fun hostStarted() {
        _backstack.forEach { it.controller.hostStarted() }
    }

    internal fun hostStopped() {
        prepareForContainerRemoval()
        _backstack.reversed().forEach { it.controller.hostStopped() }
    }

    internal fun hostIsBeingDestroyed() {
        _backstack.reversed().forEach { it.controller.isBeingDestroyed = true }
    }

    internal fun hostDestroyed() {
        _backstack.reversed().forEach { it.controller.hostDestroyed() }
        removeContainer()
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
                .map { Transaction.fromBundle(it, routerManager.controllerFactory) }
        )

        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        _backstack.forEach { moveControllerToCorrectState(it.controller) }
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
            getListeners()
        )
    }

    private fun moveControllerToCorrectState(controller: Controller) {
        controller.setRouter(this)

        if (hasContainer) {
            controller.containerSet()
        }

        if (routerManager.hostStarted) {
            controller.hostStarted()
        }

        if (routerManager.hostIsBeingDestroyed) {
            controller.isBeingDestroyed = true
        }

        if (routerManager.hostDestroyed) {
            controller.hostDestroyed()
        }
    }

    internal fun getListeners(recursiveOnly: Boolean = false): List<RouterListener> {
        return listeners
            .filter { !recursiveOnly || it.recursive }
            .map(ListenerEntry<RouterListener>::listener) +
                (routerManager.host.safeAs<Controller>()?.router?.getListeners(true)
                    ?: emptyList())
    }

    internal fun getControllerListeners(recursiveOnly: Boolean = false): List<ControllerListener> {
        return controllerListeners
            .filter { !recursiveOnly || it.recursive }
            .map(ListenerEntry<ControllerListener>::listener) +
                (routerManager.host.safeAs<Controller>()?.router?.getControllerListeners(true)
                    ?: emptyList())
    }

    private fun List<Transaction>.filterVisible(): List<Transaction> =
        takeLastUntil {
            it.pushChangeHandler != null
                    && !it.pushChangeHandler!!.removesFromViewOnPush
        }

    private data class ListenerEntry<T>(
        val listener: T,
        val recursive: Boolean
    )

    companion object {
        private const val KEY_BACKSTACK = "Router.backstack"
        private const val KEY_CONTAINER_ID = "Router.containerId"
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