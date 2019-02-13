package com.ivianuu.director

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
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
import com.ivianuu.director.ControllerState.VIEW_BOUND
import com.ivianuu.director.internal.ViewAttachHandler
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
        get() = if (routerSet) _router else error("router is only available after onCreate")

    private lateinit var _router: Router
    internal var routerSet = false

    /**
     * The host of the router this controller lives in
     */
    val host: Any
        get() = if (routerSet) router.host else error("host is only available after onCreate")

    /**
     * The view of this controller or null
     */
    var view: View? = null
        internal set

    /**
     * The instance id of this controller
     */
    var instanceId = UUID.randomUUID().toString()
        private set

    /**
     * The target controller of this controller
     */
    var targetController: Controller?
        get() = targetInstanceId?.let { _router.rootRouter.getControllerByInstanceIdOrNull(it) }
        set(value) {
            check(targetInstanceId == null) {
                "the target controller can only be set once"
            }
            targetInstanceId = value?.instanceId
        }

    private var targetInstanceId: String? = null

    /**
     * The current state of this controller
     */
    var state: ControllerState = INITIALIZED

    var isBeingDestroyed: Boolean = false
        internal set(value) {
            if (value) childRouterManager.hostIsBeingDestroyed()
            field = value
        }

    private var allState: Bundle? = null
    private var instanceState: Bundle? = null
    private var viewState: Bundle? = null

    private var hasSavedViewState = false
    private var viewIsAttached = false
    private var attachedToUnownedParent = false

    /**
     * Whether or not the view should be retained while being detached
     */
    var retainView = false
        set(value) {
            field = value
            if (!value && !isAttached) {
                unbindView()
            }
        }

    /**
     * All child routers of this controller
     */
    val childRouters: List<Router>
        get() = childRouterManager.routers
    val childRouterManager by lazy(LazyThreadSafetyMode.NONE) {
        check(routerSet) { "Cannot access child routers before onCreate" }
        RouterManager(this, router, postponeFullRestore = true)
    }

    private val lifecycleListeners = mutableListOf<ControllerLifecycleListener>()

    private val attachHandler = ViewAttachHandler { attached ->
        viewIsAttached = attached

        if (attached) {
            attach()
        } else {
            detach()

            // this means that a controller was pushed on top of us
            if (!attachedToUnownedParent && !isBeingDestroyed && !retainView) {
                unbindView()
            }
        }
    }

    private var superCalled = false

    private var hostStarted = false
    private var awaitingHostStart = false

    /**
     * Will be called once when the router was set for the first time
     */
    protected open fun onCreate(savedInstanceState: Bundle?) {
        // restore the full instance state of child routers
        childRouterManager.startPostponedFullRestore()

        superCalled = true
    }

    /**
     * Returns the view for this controller
     */
    protected abstract fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View

    /**
     * Called after the of this controller was created
     */
    protected open fun onBindView(view: View, savedViewState: Bundle?) {
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
     * Called when the view of this controller gets destroyed
     */
    protected open fun onUnbindView(view: View) {
        superCalled = true
    }

    /**
     * Called when this Controller has been destroyed.
     */
    protected open fun onDestroy() {
        superCalled = true
    }

    /**
     * Called when this Controller begins the process of being swapped in or out of the host view.
     */
    protected open fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        superCalled = true
    }

    /**
     * Called when this Controller completes the process of being swapped in or out of the host view.
     */
    protected open fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        superCalled = true
    }


    /**
     * Called to save the view state of this controller
     */
    protected open fun onSaveViewState(view: View, outState: Bundle) {
        superCalled = true
    }

    /**
     * Called to save the instance state of this controller
     */
    protected open fun onSaveInstanceState(outState: Bundle) {
        superCalled = true
    }

    /**
     * Should be overridden if this Controller needs to handle the back button being pressed.
     */
    open fun handleBack(): Boolean = childRouterManager.handleBack()

    /**
     * Adds a listener for all of this Controller's lifecycle events
     */
    fun addLifecycleListener(listener: ControllerLifecycleListener) {
        if (!lifecycleListeners.contains(listener)) {
            lifecycleListeners.add(listener)
        }
    }

    /**
     * Removes a previously added lifecycle listener
     */
    fun removeLifecycleListener(listener: ControllerLifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    internal fun setRouter(router: Router) {
        if (routerSet) return
        routerSet = true
        _router = router

        // restore the internal state
        allState?.let { restoreInstanceState() }
        allState = null

        // create
        if (!isCreated) {
            notifyLifecycleListeners { it.preCreate(this, instanceState) }

            state = CREATED

            requireSuperCalled { onCreate(instanceState) }

            notifyLifecycleListeners { it.postCreate(this, instanceState) }

            instanceState = null
        }
    }

    internal fun containerAttached() {
    }

    internal fun hostStarted() {
        hostStarted = true
        attach()
    }

    internal fun hostStopped() {
        hostStarted = false

        detach()

        // cancel any pending input event
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view?.cancelPendingInputEvents()
        }
    }

    internal fun containerDetached() {
        // decide whether or not our view should be retained
        if (view != null && (isBeingDestroyed || !retainView)) {
            unbindView()
        }
    }

    internal fun hostDestroyed() {
        if (state == DESTROYED) return

        childRouterManager.hostDestroyed()

        notifyLifecycleListeners { it.preDestroy(this) }

        state = DESTROYED

        requireSuperCalled { onDestroy() }

        notifyLifecycleListeners { it.postDestroy(this) }
    }

    internal fun inflate(parent: ViewGroup): View {
        var view = view

        if (view == null) {
            notifyLifecycleListeners { it.preInflateView(this, viewState) }

            view = onInflateView(
                LayoutInflater.from(parent.context),
                parent,
                viewState
            ).also { this.view = it }

            notifyLifecycleListeners { it.postInflateView(this, view, viewState) }

            val viewState = viewState

            if (viewState != null) {
                view.restoreHierarchyState(
                    viewState.getSparseParcelableArray(
                        KEY_VIEW_STATE_HIERARCHY
                    )
                )
                val savedViewState = viewState.getBundle(KEY_VIEW_STATE_BUNDLE)!!
                savedViewState.classLoader = javaClass.classLoader
            }

            this.viewState = null

            notifyLifecycleListeners { it.preBindView(this, view, viewState) }

            state = VIEW_BOUND

            requireSuperCalled { onBindView(view, viewState) }

            notifyLifecycleListeners { it.postBindView(this, view, viewState) }

            attachHandler.takeView(view)

            view.safeAs<ViewGroup>()?.let { childRouterManager.setContainers(it) }
        } else if (retainView) {
            view.safeAs<ViewGroup>()?.let { childRouterManager.setContainers(it) }
        }

        return view
    }

    private fun attach() {
        val view = view ?: return

        if (isAttached) return

        if (!viewIsAttached) return

        attachedToUnownedParent = view.parent != router.container

        // this can happen while transitions just ignore it
        if (attachedToUnownedParent) return

        if (!hostStarted) {
            awaitingHostStart = true
            return
        } else {
            awaitingHostStart = false
        }

        notifyLifecycleListeners { it.preAttach(this, view) }

        state = ATTACHED

        requireSuperCalled { onAttach(view) }

        notifyLifecycleListeners { it.postAttach(this, view) }

        hasSavedViewState = false

        childRouterManager.hostStarted()
    }

    private fun detach() {
        val view = view ?: return

        if (attachedToUnownedParent) return

        if (isAttached) {
            childRouterManager.hostStopped()

            notifyLifecycleListeners { it.preDetach(this, view) }

            state = VIEW_BOUND

            requireSuperCalled { onDetach(view) }

            notifyLifecycleListeners { it.postDetach(this, view) }
        }
    }

    private fun unbindView() {
        val view = view ?: return
        if (!isBeingDestroyed && !hasSavedViewState) {
            saveViewState()
        }

        childRouterManager.removeContainers()

        notifyLifecycleListeners { it.preUnbindView(this, view) }

        requireSuperCalled { onUnbindView(view) }

        attachHandler.dropView(view)

        this.view = null

        notifyLifecycleListeners { it.postUnbindView(this) }
    }

    internal fun saveInstanceState(): Bundle {
        val view = view
        if (!hasSavedViewState && view != null) {
            saveViewState()
        }

        val outState = Bundle()
        outState.putString(KEY_CLASS_NAME, javaClass.name)
        outState.putBundle(KEY_VIEW_STATE, viewState)
        outState.putBundle(KEY_ARGS, args)
        outState.putString(KEY_INSTANCE_ID, instanceId)
        outState.putString(KEY_TARGET_INSTANCE_ID, instanceId)
        outState.putBoolean(KEY_RETAIN_VIEW, retainView)

        val savedState = Bundle(javaClass.classLoader)
        requireSuperCalled { onSaveInstanceState(savedState) }

        notifyLifecycleListeners { it.onSaveInstanceState(this, savedState) }

        outState.putBundle(KEY_SAVED_STATE, savedState)

        outState.putBundle(KEY_CHILD_ROUTER_STATES, childRouterManager.saveInstanceState())

        return outState
    }

    private fun restoreInstanceState() {
        val savedInstanceState = allState ?: return

        savedInstanceState.getBundle(KEY_ARGS)?.let { bundle ->
            args = bundle.apply { classLoader = this@Controller.javaClass.classLoader }
        }

        instanceState = savedInstanceState.getBundle(KEY_SAVED_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        viewState = savedInstanceState.getBundle(KEY_VIEW_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        instanceId = savedInstanceState.getString(KEY_INSTANCE_ID)!!
        targetInstanceId = savedInstanceState.getString(KEY_TARGET_INSTANCE_ID)

        retainView = savedInstanceState.getBoolean(KEY_RETAIN_VIEW)

        childRouterManager.restoreInstanceState(savedInstanceState.getBundle(KEY_CHILD_ROUTER_STATES))
    }

    private fun saveViewState() {
        val view = view ?: return

        hasSavedViewState = true

        val viewState = Bundle(javaClass.classLoader).also { this.viewState = it }

        val hierarchyState = SparseArray<Parcelable>()
        view.saveHierarchyState(hierarchyState)
        viewState.putSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY, hierarchyState)

        val stateBundle = Bundle(javaClass.classLoader)
        requireSuperCalled { onSaveViewState(view, stateBundle) }
        viewState.putBundle(KEY_VIEW_STATE_BUNDLE, stateBundle)

        notifyLifecycleListeners { it.onSaveViewState(this, viewState) }
    }

    internal fun changeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeStarted(changeHandler, changeType)
        notifyLifecycleListeners { it.onChangeStart(this, changeHandler, changeType) }
    }

    internal fun changeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        onChangeEnded(changeHandler, changeType)
        notifyLifecycleListeners { it.onChangeEnd(this, changeHandler, changeType) }
    }

    private inline fun notifyLifecycleListeners(block: (ControllerLifecycleListener) -> Unit) {
        val listeners = lifecycleListeners + router.getAllLifecycleListeners()
        listeners.forEach(block)
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
        private const val KEY_SAVED_STATE = "Controller.instanceState"
        private const val KEY_INSTANCE_ID = "Controller.instanceId"
        private const val KEY_TARGET_INSTANCE_ID = "Controller.targetInstanceId"
        private const val KEY_ARGS = "Controller.args"
        private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
        private const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"
        private const val KEY_RETAIN_VIEW = "Controller.retainViewMode"

        internal fun fromBundle(bundle: Bundle, factory: ControllerFactory): Controller {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            val args = bundle.getBundle(KEY_ARGS)
            return factory.createController(
                Controller::class.java.classLoader!!, className, args!!
            ).apply {
                allState = bundle
            }
        }
    }
}

/**
 * Whether or not [Controller.state] is at least [CREATED]
 */
val Controller.isCreated: Boolean get() = state.isAtLeast(CREATED)

/**
 * Whether or not [Controller.state] is at least [VIEW_BOUND]
 */
val Controller.isViewBound: Boolean get() = state.isAtLeast(VIEW_BOUND)

/**
 * Whether or not [Controller.state] is at least [VIEW_BOUND]
 */
val Controller.isAttached: Boolean get() = state.isAtLeast(ATTACHED)

/**
 * Whether or not [Controller.state] is [DESTROYED]
 */
val Controller.isDestroyed: Boolean get() = state == DESTROYED

/**
 * Whether or not this controller has a view
 */
val Controller.hasView: Boolean get() = view != null

/**
 * The parent controller of this controller or null
 */
val Controller.parentController: Controller?
    get() = host as? Controller

/**
 * The context of this controller if any of this or parent controllers is attached to one
 */
val Controller.context: Context
    get() {
        return if (host is Context) {
            host as Context
        } else {
            (host as? Controller)?.context
        } ?: error("no context found")
    }

/**
 * The context of this controller if any of this or parent controllers is attached to one
 */
val Controller.activity: Activity
    get() = context as? Activity ?: error("no activity found")

/**
 * The application of the attached activity
 */
val Controller.application: Application
    get() = context.applicationContext as Application

/**
 * The resources of the attached activity
 */
val Controller.resources: Resources get() = context.resources

/**
 * Starts the [intent]
 */
fun Controller.startActivity(intent: Intent) {
    context.startActivity(intent)
}

/**
 * Returns a new router transaction
 */
fun Controller.toTransaction(): RouterTransaction = RouterTransaction(this)

/**
 * Returns the child router for [containerId] and [tag] or null
 */
fun Controller.getChildRouterOrNull(
    containerId: Int,
    tag: String?
): Router? = childRouterManager.getRouterOrNull(containerId, tag)

/**
 * Returns the child router for [containerId] and [tag]
 */
fun Controller.getChildRouter(
    containerId: Int,
    tag: String? = null
): Router = childRouterManager.getRouter(containerId, tag)

/**
 * Removes the [childRouter]. All Controllers currently managed by
 * the [childRouter] will be destroyed.
 */
fun Controller.removeChildRouter(childRouter: Router) {
    childRouterManager.removeRouter(childRouter)
}

/**
 * Returns the child router for [container] and [tag] or null
 */
fun Controller.getChildRouterOrNull(
    container: ViewGroup,
    tag: String? = null
): Router? = getChildRouterOrNull(container.id, tag)

/**
 * Returns the child router for [container] and [tag] or creates a new instance
 */
fun Controller.getChildRouter(
    container: ViewGroup,
    tag: String? = null
): Router = getChildRouter(container.id, tag)