package com.ivianuu.director

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.ControllerState.ATTACHED
import com.ivianuu.director.ControllerState.CREATED
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.ControllerState.INITIALIZED
import com.ivianuu.director.ControllerState.VIEW_CREATED
import com.ivianuu.director.internal.classForNameOrThrow
import com.ivianuu.stdlibx.safeAs
import java.util.*

/**
 * Lightweight view controller with a lifecycle
 */
abstract class Controller {

    /**
     * The arguments of this controller
     */
    var args = Bundle(javaClass.classLoader)

    /**
     * The router of this controller
     */
    val router: Router
        get() {
            check(this::_router.isInitialized) {
                "router is only available after onCreate"
            }

            return _router
        }

    private lateinit var _router: Router

    /**
     * The view of this controller or null
     */
    var view: View? = null
        private set

    /**
     * The instance id of this controller
     */
    var instanceId = UUID.randomUUID().toString()
        private set

    /**
     * The current state of this controller
     */
    var state: ControllerState = INITIALIZED
        private set

    /**
     * Whether or not this controller is in the process of being destroyed
     */
    var isBeingDestroyed = false
        internal set(value) {
            if (value) childRouterManager.hostIsBeingDestroyed()
            field = value
        }

    private var isPerformingExitTransition = false

    private var allState: Bundle? = null
    private var viewState: Bundle? = null

    /**
     * Whether or not the view should be retained while being detached
     */
    var retainView = DirectorPlugins.defaultRetainView
        set(value) {
            field = value
            if (!value && !isAttached) {
                destroyView()
            }
        }

    /**
     * The child router manager of this controller
     */
    val childRouterManager = RouterManager(this, postponeFullRestore = true)

    private val listeners = mutableListOf<ControllerListener>()

    private val viewAttachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            attach()
        }

        override fun onViewDetachedFromWindow(v: View) {
            detach()
        }
    }

    private var superCalled = false

    /**
     * Will be called when this controller gets attached to its router
     */
    protected open fun onCreate(savedInstanceState: Bundle?) {
        superCalled = true
        // restore the full instance state of child routers
        childRouterManager.startPostponedFullRestore()
    }

    /**
     * Called when this Controller has been destroyed.
     */
    protected open fun onDestroy() {
        superCalled = true
    }

    /**
     * Creates the view for this controller
     */
    protected abstract fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View

    /**
     * Called when the view of this controller gets destroyed
     */
    protected open fun onDestroyView(view: View) {
        superCalled = true
    }

    /**
     * Called when this Controller is attached to its container
     */
    protected open fun onAttach(view: View) {
        superCalled = true
    }

    /**
     * Called when this Controller is detached from its container
     */
    protected open fun onDetach(view: View) {
        superCalled = true
    }

    /**
     * Called when this Controller begins the process of being swapped in or out of the host view.
     */
    protected open fun onChangeStarted(
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        superCalled = true
    }

    /**
     * Called when this Controller completes the process of being swapped in or out of the host view.
     */
    protected open fun onChangeEnded(
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        superCalled = true
    }

    /**
     * Will be called when the instance state gets restores
     */
    protected open fun onRestoreInstanceState(savedInstanceState: Bundle) {
        superCalled = true
    }

    /**
     * Called to save the instance state of this controller
     */
    protected open fun onSaveInstanceState(outState: Bundle) {
        superCalled = true
    }

    /**
     * Will be called when the view state gets restored
     */
    protected open fun onRestoreViewState(view: View, savedViewState: Bundle) {
        superCalled = true
    }

    /**
     * Called to save the view state of this controller
     */
    protected open fun onSaveViewState(view: View, outState: Bundle) {
        superCalled = true
    }

    /**
     * Should be overridden if this Controller needs to handle the back button being pressed.
     */
    open fun handleBack(): Boolean = childRouterManager.handleBack()

    /**
     * Adds a listener for all of this Controller's lifecycle events
     */
    fun addListener(listener: ControllerListener) {
        listeners.add(listener)
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeListener(listener: ControllerListener) {
        listeners.remove(listener)
    }

    internal fun setRouter(router: Router) {
        if (this::_router.isInitialized) return
        _router = router

        val instanceState = allState?.getBundle(KEY_SAVED_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        // create
        notifyListeners { it.preCreate(this, instanceState) }

        state = CREATED

        requireSuperCalled { onCreate(instanceState) }

        notifyListeners { it.postCreate(this, instanceState) }

        // restore the instance state
        if (instanceState != null) {
            requireSuperCalled { onRestoreInstanceState(instanceState) }
            notifyListeners { it.onRestoreInstanceState(this, instanceState) }
        }

        allState = null
    }

    internal fun containerSet() {
    }

    internal fun hostStarted() {
        attach()
    }

    internal fun hostStopped() {
        detach()
    }

    internal fun containerRemoved() {
        // decide whether or not our view should be retained
        val view = view ?: return

        if (isBeingDestroyed || !retainView) {
            destroyView()
        } else if (retainView) {
            // remove containers to let children
            // decide whether they wanna keep their view or not
            childRouterManager.removeContainers()
            // remove our retained view from the parent
            view.parent.safeAs<ViewGroup>()?.removeView(view)
        }
    }

    internal fun hostDestroyed() {
        if (state == DESTROYED) return

        childRouterManager.hostDestroyed()

        notifyListeners { it.preDestroy(this) }

        state = DESTROYED

        requireSuperCalled(this::onDestroy)

        notifyListeners { it.postDestroy(this) }
    }

    internal fun createView(container: ViewGroup): View {
        var view = view

        // return the retained view
        if (view != null) {
            // restore containers
            if (retainView) {
                view.safeAs<ViewGroup>()?.let(childRouterManager::setContainers)
            }
            return view
        }

        val hierarchyState =
            viewState?.getSparseParcelableArray<Parcelable>(KEY_VIEW_STATE_HIERARCHY)
        val savedViewState = viewState?.getBundle(KEY_VIEW_STATE_BUNDLE)

        notifyListeners { it.preCreateView(this, savedViewState) }

        view = onCreateView(
            LayoutInflater.from(container.context),
            container,
            savedViewState
        ).also { this.view = it }

        state = VIEW_CREATED

        notifyListeners { it.postCreateView(this, view, savedViewState) }

        if (savedViewState != null) {
            view.restoreHierarchyState(hierarchyState)

            requireSuperCalled { onRestoreViewState(view, savedViewState) }
            notifyListeners { it.onRestoreViewState(this, view, savedViewState) }
        }

        viewState = null

        view.addOnAttachStateChangeListener(viewAttachListener)

        view.safeAs<ViewGroup>()?.let(childRouterManager::setContainers)

        return view
    }

    private fun attach() {
        if (isAttached) return

        val view = view ?: return

        // View.isAttached
        if (view.windowToken == null) return

        if (!routerManager.hostStarted) return

        notifyListeners { it.preAttach(this, view) }

        state = ATTACHED

        requireSuperCalled { onAttach(view) }

        notifyListeners { it.postAttach(this, view) }

        viewState = null

        childRouterManager.hostStarted()
    }

    private fun detach() {
        if (!isAttached) return

        val view = view ?: return

        childRouterManager.hostStopped()

        notifyListeners { it.preDetach(this, view) }

        state = VIEW_CREATED

        requireSuperCalled { onDetach(view) }

        notifyListeners { it.postDetach(this, view) }

        if (isPerformingExitTransition) {
            if (isBeingDestroyed) {
                containerRemoved()
            } else {
                if (!retainView) {
                    destroyView()
                } else {
                    childRouterManager.removeContainers()
                }
            }
        }
    }

    private fun destroyView() {
        val view = view ?: return
        if (!isBeingDestroyed && viewState == null) {
            saveViewState()
        }

        childRouterManager.removeContainers()

        notifyListeners { it.preDestroyView(this, view) }

        requireSuperCalled { onDestroyView(view) }

        view.removeOnAttachStateChangeListener(viewAttachListener)

        this.view = null

        notifyListeners { it.postDestroyView(this) }

        if (isBeingDestroyed && isPerformingExitTransition) {
            hostDestroyed()
        }
    }

    internal fun saveInstanceState(): Bundle {
        if (view != null && viewState == null) {
            saveViewState()
        }

        val outState = Bundle()
        outState.putString(KEY_CLASS_NAME, javaClass.name)
        outState.putBundle(KEY_VIEW_STATE, viewState)
        outState.putBundle(KEY_ARGS, args)
        outState.putString(KEY_INSTANCE_ID, instanceId)
        outState.putBoolean(KEY_RETAIN_VIEW, retainView)

        val savedState = Bundle(javaClass.classLoader)
        requireSuperCalled { onSaveInstanceState(savedState) }
        notifyListeners { it.onSaveInstanceState(this, savedState) }
        outState.putBundle(KEY_SAVED_STATE, savedState)

        outState.putBundle(KEY_CHILD_ROUTER_STATES, childRouterManager.saveInstanceState())

        return outState
    }

    private fun restoreInstanceState() {
        val savedInstanceState = allState ?: return

        savedInstanceState.getBundle(KEY_ARGS)?.let { bundle ->
            args = bundle.also { it.classLoader = javaClass.classLoader }
        }

        viewState = savedInstanceState.getBundle(KEY_VIEW_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        instanceId = savedInstanceState.getString(KEY_INSTANCE_ID)!!

        retainView = savedInstanceState.getBoolean(KEY_RETAIN_VIEW)

        childRouterManager.restoreInstanceState(savedInstanceState.getBundle(KEY_CHILD_ROUTER_STATES))
    }

    private fun saveViewState() {
        val view = view ?: return

        val viewState = Bundle(javaClass.classLoader).also { this.viewState = it }

        val hierarchyState = SparseArray<Parcelable>()
        view.saveHierarchyState(hierarchyState)
        viewState.putSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY, hierarchyState)

        val stateBundle = Bundle(javaClass.classLoader)
        requireSuperCalled { onSaveViewState(view, stateBundle) }
        viewState.putBundle(KEY_VIEW_STATE_BUNDLE, stateBundle)

        notifyListeners { it.onSaveViewState(this, view, viewState) }
    }

    internal fun changeStarted(
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        isPerformingExitTransition = !changeType.isEnter
        requireSuperCalled { onChangeStarted(other, changeHandler, changeType) }
        notifyListeners { it.onChangeStarted(this, other, changeHandler, changeType) }
    }

    internal fun changeEnded(
        other: Controller?,
        changeHandler: ChangeHandler,
        changeType: ControllerChangeType
    ) {
        requireSuperCalled { onChangeEnded(other, changeHandler, changeType) }
        notifyListeners { it.onChangeEnded(this, other, changeHandler, changeType) }
        isPerformingExitTransition = false
    }

    private inline fun notifyListeners(block: (ControllerListener) -> Unit) {
        (listeners + router.getControllerListeners()).forEach(block)
    }

    private inline fun requireSuperCalled(block: () -> Unit) {
        superCalled = false
        block()
        check(superCalled) { "super not called ${javaClass.name}" }
    }

    companion object {
        private const val KEY_CLASS_NAME = "Controller.className"
        private const val KEY_VIEW_STATE = "Controller.viewState"
        private const val KEY_CHILD_ROUTER_STATES = "Controller.childRouterStates"
        private const val KEY_SAVED_STATE = "Controller.savedState"
        private const val KEY_INSTANCE_ID = "Controller.instanceId"
        private const val KEY_ARGS = "Controller.args"
        private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
        private const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"
        private const val KEY_RETAIN_VIEW = "Controller.retainViewMode"

        internal fun fromBundle(bundle: Bundle, factory: ControllerFactory): Controller {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            val cls = classForNameOrThrow(className)

            val args = bundle.getBundle(KEY_ARGS)!!.apply {
                classLoader = cls.classLoader
            }

            return factory.createController(cls.classLoader!!, className).apply {
                this.args = args
                this.allState = bundle
                restoreInstanceState()
            }
        }
    }
}

val Controller.isCreated: Boolean get() = state.isAtLeast(CREATED)

val Controller.isViewCreated: Boolean get() = state.isAtLeast(VIEW_CREATED)

val Controller.isAttached: Boolean get() = state.isAtLeast(ATTACHED)

val Controller.isDestroyed: Boolean get() = state == DESTROYED

val Controller.transaction: Transaction
    get() = router.backstack.first { it.controller == this }

val Controller.routerManager: RouterManager
    get() = router.routerManager

val Controller.host: Any
    get() = routerManager.host

val Controller.parentController: Controller?
    get() = host as? Controller

val Controller.context: Context
    get() {
        return if (host is Context) {
            host as Context
        } else {
            (host as? Controller)?.context
        } ?: error("no context found")
    }

val Controller.activity: Activity
    get() = context as? Activity ?: error("no activity found")

val Controller.application: Application
    get() = context.applicationContext as Application

val Controller.resources: Resources get() = context.resources

fun Controller.startActivity(intent: Intent) {
    context.startActivity(intent)
}

val Controller.childRouters: List<Router> get() = childRouterManager.routers

fun Controller.getChildRouterOrNull(
    containerId: Int,
    tag: String?
): Router? = childRouterManager.getRouterOrNull(containerId, tag)

fun Controller.getChildRouter(
    containerId: Int,
    tag: String? = null
): Router = childRouterManager.getRouter(containerId, tag)

fun Controller.removeChildRouter(childRouter: Router) {
    childRouterManager.removeRouter(childRouter)
}

fun Controller.getChildRouterOrNull(
    container: ViewGroup,
    tag: String? = null
): Router? = getChildRouterOrNull(container.id, tag)

fun Controller.getChildRouter(
    container: ViewGroup,
    tag: String? = null
): Router = getChildRouter(container.id, tag)

fun Controller.childRouter(
    containerId: Int,
    tag: String? = null
): Lazy<Router> = lazy(LazyThreadSafetyMode.NONE) { getChildRouter(containerId, tag) }

fun Controller.toTransaction(): Transaction = Transaction(this)