package com.ivianuu.director

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.internal.ChildRouter
import com.ivianuu.director.internal.ControllerAttachHandler
import java.util.*
import kotlin.collections.ArrayList

/**
 * A Controller manages portions of the UI. It is similar to an Activity or Fragment in that it manages its
 * own lifecycle and controls interactions between the UI and whatever logic is required. It is, however,
 * a much lighter weight component than either Activities or Fragments. While it offers several lifecycle
 * methods, they are much simpler and more predictable than those of Activities and Fragments.
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
    private var routerSet = false

    /**
     * Returns the host activity of this controller
     */
    val activity: Activity
        get() = if (routerSet) router.activity else error("activity is only available after onCreate")

    /**
     * The view of this controller or null
     */
    var view: View? = null
        private set

    /**
     * The parent controller of this controller or null
     */
    var parentController: Controller? = null
        private set

    /**
     * The instance id of this controller
     */
    var instanceId = UUID.randomUUID().toString()
        private set

    /**
     * The target controller of this controller
     */
    var targetController: Controller?
        get() = targetInstanceId?.let { _router.rootRouter.findControllerByInstanceId(it) }
        set(value) {
            check(targetInstanceId == null) {
                "the target controller can only be set once"
            }
            targetInstanceId = value?.instanceId
        }

    private var targetInstanceId: String? = null

    /**
     * Whether or not this controller is already created
     */
    var isCreated = false
        private set

    /**
     * Whether or not this Controller is currently attached to its container.
     */
    var isAttached = false
        private set

    /**
     * Whether or not this Controller has been destroyed.
     */
    var isDestroyed = false
        private set

    /**
     * Whether or not this Controller is currently in the process of being destroyed.
     */
    var isBeingDestroyed = false
        private set

    private var allState: Bundle? = null
    private var instanceState: Bundle? = null
    private var viewState: Bundle? = null
    private var childRouterStates: Map<ChildRouter, Bundle>? = null

    private var viewFullyCreated = false
    private var hasSavedViewState = false

    /**
     * Whether or not the view should be retained while being detached
     */
    var retainView = false
        set(value) {
            field = value
            if (!value && !isAttached) {
                unbindView(false)
            }
        }

    /**
     * All child routers of this controller
     */
    val childRouters: List<Router>
        get() = _childRouters.toList()
    private val _childRouters = mutableListOf<ChildRouter>()

    private val lifecycleListeners = mutableSetOf<ControllerLifecycleListener>()

    private val attachHandler by lazy(LazyThreadSafetyMode.NONE) {
        ControllerAttachHandler(parentController != null, ::handleAttachStateChange)
    }

    private var superCalled = false

    /**
     * Will be called once when the router was set for the first time
     */
    protected open fun onCreate(savedInstanceState: Bundle?) {
        // restore the full instance state of child routers
        childRouterStates
            ?.filterKeys { _childRouters.contains(it) }
            ?.forEach { it.key.restoreInstanceState(it.value) }
        childRouterStates = null

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
    open fun handleBack(): Boolean {
        return _childRouters
            .flatMap { it.backstack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .map { it.controller }
            .any { it.isAttached && it.router.handleBack() }
    }

    /**
     * Adds a listener for all of this Controller's lifecycle events
     */
    fun addLifecycleListener(listener: ControllerLifecycleListener) {
        lifecycleListeners.add(listener)
    }

    /**
     * Removes a previously added lifecycle listener
     */
    fun removeLifecycleListener(listener: ControllerLifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    /**
     * Returns the child router for [containerId] and [tag]
     */
    fun getChildRouter(
        containerId: Int,
        tag: String? = null,
        controllerFactory: ControllerFactory? = null
    ): Router {
        var childRouter = _childRouters
            .firstOrNull { it.hostId == containerId && it.tag == tag }

        if (childRouter == null) {
            childRouter = ChildRouter(
                this,
                containerId,
                tag
            )

            childRouter.controllerFactory = controllerFactory

            _childRouters.add(childRouter)

            if (router.hostStarted) {
                childRouter.hostStarted()
            }
        }

        restoreChildControllerContainers()

        return childRouter
    }

    /**
     * Removes the [childRouter]. All Controllers currently managed by
     * the [childRouter] will be destroyed.
     */
    fun removeChildRouter(childRouter: Router) {
        if (_childRouters.remove(childRouter)) {
            childRouter.destroy(true)
        }
    }

    internal fun findController(instanceId: String): Controller? {
        if (this.instanceId == instanceId) return this

        return childRouters
            .map { it.findControllerByInstanceId(instanceId) }
            .firstOrNull()
    }

    /**
     * Sets the initial state of this controller which was previously created by
     * [Router.saveControllerInstanceState]
     */
    fun setInitialSavedState(state: Bundle?) {
        check(!routerSet) { "controller already added" }

        if (state != null) {
            val className = state.getString(KEY_CLASS_NAME)
            require(javaClass.name == className) {
                "state of $className cannot be used for ${javaClass.name}"
            }
        }

        allState = state
    }

    internal fun setRouter(router: Router) {
        if (routerSet) return
        routerSet = true
        _router = router

        // restore the internal state
        allState?.let { restoreInstanceState() }
        allState = null

        // create
        create()
    }

    internal fun setParentController(parentController: Controller) {
        this.parentController = parentController
    }

    internal fun hostStarted() {
        attachHandler.hostStarted()
        _childRouters.forEach { it.hostStarted() }
    }

    internal fun hostStopped() {
        _childRouters.forEach { it.hostStopped() }

        attachHandler.hostStopped()

        // cancel any pending input event
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view?.cancelPendingInputEvents()
        }
    }

    internal fun hostDestroyed() {
        _childRouters.forEach { it.hostDestroyed() }
        destroy(true)
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

            notifyLifecycleListeners { it.postInflateView(this, view, viewState) }

            notifyLifecycleListeners { it.preBindView(this, view, viewState) }

            requireSuperCalled { onBindView(view, viewState) }

            notifyLifecycleListeners { it.postBindView(this, view, viewState) }

            viewFullyCreated = true

            restoreChildControllerContainers()

            attachHandler.takeView(view)
        } else if (retainView) {
            restoreChildControllerContainers()
        }

        return view
    }

    private fun handleAttachStateChange(
        reason: ControllerAttachHandler.ChangeReason,
        viewAttached: Boolean,
        parentAttached: Boolean,
        hostStarted: Boolean
    ) {
        val view = view ?: return

        if (viewAttached && parentAttached && hostStarted) {
            // explicitly check the container
            // we could get attached to another container while transitioning
            if (view.parent == router.container) {
                attach()
            }
        } else {
            detach()

            if (!viewAttached) {
                val parentController = parentController
                oldOnDetachCode(
                    forceViewRemoval = false,
                    blockViewRemoval = reason != ControllerAttachHandler.ChangeReason.VIEW ||
                            parentController != null && parentController.isBeingDestroyed
                    ,
                    forceChildViewRemoval = false,
                    fromHostRemoval = false
                )

                if (isBeingDestroyed && (parentController == null
                            || !parentController.isBeingDestroyed)
                ) {
                    unbindView(true)
                    performDestroy()
                }
            }
        }
    }

    private fun attach() {
        val view = view ?: return

        notifyLifecycleListeners { it.preAttach(this, view) }

        isAttached = true

        requireSuperCalled { onAttach(view) }

        notifyLifecycleListeners { it.postAttach(this, view) }

        hasSavedViewState = false

        _childRouters
            .flatMap { it.backstack }
            .forEach { it.controller.parentAttached() }
    }

    private fun detach() {
        val view = view ?: return

        if (isAttached) {
            _childRouters
                .flatMap { it.backstack }
                .forEach { it.controller.parentDetached() }

            notifyLifecycleListeners { it.preDetach(this, view) }
            isAttached = false

            requireSuperCalled { onDetach(view) }

            notifyLifecycleListeners { it.postDetach(this, view) }
        }
    }

    internal fun parentAttached() {
        attachHandler.parentAttached()
    }

    internal fun parentDetached() {
        attachHandler.parentDetached()
    }

    internal fun oldOnDetachCode(
        forceViewRemoval: Boolean,
        blockViewRemoval: Boolean,
        forceChildViewRemoval: Boolean,
        fromHostRemoval: Boolean
    ) {
        val view = view ?: return

        if (isAttached) {
            detach()
        }

        val parentController = parentController
        val removeViewRef =
            !blockViewRemoval && (forceViewRemoval || !retainView || (isBeingDestroyed
                    && (parentController == null || !parentController.isBeingDestroyed)))

        if (removeViewRef) {
            unbindView(forceChildViewRemoval)
        } else if (retainView && fromHostRemoval) {
            // this happens if we are a child controller, have RETAIN_DETACH
            // and the parent does not RETAIN_DETACH
            // we remove the view from the container to make sure that
            // we don't re-inflate the view in Controller.inflate
            // because we would be attached to the old container otherwise
            (view.parent as? ViewGroup)?.removeView(view)
        }
    }

    private fun unbindView(forceChildViewRemoval: Boolean) {
        val view = view
        if (view != null) {
            if (!isBeingDestroyed && !hasSavedViewState) {
                saveViewState()
            }

            _childRouters.forEach { it.removeContainer(forceChildViewRemoval) }

            notifyLifecycleListeners { it.preUnbindView(this, view) }

            requireSuperCalled { onUnbindView(view) }

            attachHandler.dropView(view)

            this.view = null
            viewFullyCreated = false

            notifyLifecycleListeners { it.postUnbindView(this) }
        }
    }

    private fun restoreChildControllerContainers() {
        val view = view

        // we check here if were in a fully created view state
        // because call child router methods in onBindView
        // would cause the child controller view to be fully created
        // before our view is fully created
        if (view != null && viewFullyCreated) {
            _childRouters
                .filterNot { it.hasContainer }
                .forEach {
                    val containerView = view.findViewById(it.hostId) as? ViewGroup ?: return@forEach
                    it.container = containerView
                    it.rebind()
                }
        }
    }

    internal fun isBeingDestroyed() {
        isBeingDestroyed = true
        _childRouters.forEach { it.isBeingDestroyed() }

        // todo combine with destroy(boolean)
        val parentController = parentController
        if (parentController == null || !parentController.isBeingDestroyed) {
            if (!isAttached && view != null) {
                unbindView(true)
            } else if (view == null) {
                performDestroy()
            }
        }
    }

    private fun destroy(removeView: Boolean) {
        isBeingDestroyed = true
        _childRouters.forEach { it.destroy(false) }

        if (!isAttached) {
            unbindView(true)
        } else if (removeView) {
            view?.let {
                detach()
                oldOnDetachCode(
                    forceViewRemoval = true,
                    blockViewRemoval = false,
                    forceChildViewRemoval = true,
                    fromHostRemoval = false
                )
            }
        }

        performDestroy()
    }

    private fun create() {
        if (isCreated) return
        notifyLifecycleListeners { it.preCreate(this, instanceState) }

        isCreated = true

        requireSuperCalled { onCreate(instanceState) }

        notifyLifecycleListeners { it.postCreate(this, instanceState) }

        instanceState = null
    }

    private fun performDestroy() {
        if (isDestroyed) return

        _childRouters.forEach { it.hostDestroyed() }

        notifyLifecycleListeners { it.preDestroy(this) }
        isDestroyed = true

        requireSuperCalled { onDestroy() }

        parentController = null
        notifyLifecycleListeners { it.postDestroy(this) }
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

        val childIdentities = _childRouters.map { it.saveIdentity() }
        val childStates = _childRouters.map { it.saveInstanceState() }

        outState.putParcelableArrayList(KEY_CHILD_ROUTER_IDENTITIES, ArrayList(childIdentities))
        outState.putParcelableArrayList(KEY_CHILD_ROUTER_STATES, ArrayList(childStates))

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

        val childIdentities = savedInstanceState.getParcelableArrayList<Bundle>(
            KEY_CHILD_ROUTER_IDENTITIES
        )!!

        val childStates = savedInstanceState
            .getParcelableArrayList<Bundle>(KEY_CHILD_ROUTER_STATES)!!

        childRouterStates = childIdentities.zip(childStates)
            .map { (identity, state) ->
                ChildRouter(this).apply {
                    // we only restore the identity for now
                    // to give the user a chance to set a [ControllerFactory] in [onCreate]
                    restoreIdentity(identity)
                } to state
            }
            .onEach { _childRouters.add(it.first) }
            .toMap()
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
        private const val KEY_CHILD_ROUTER_IDENTITIES = "Controller.childRouterIdentities"
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
 * The application of the attached activity
 */
val Controller.application: Application
    get() = activity.application

/**
 * The resources of the attached activity
 */
val Controller.resources: Resources get() = activity.resources

/**
 * Starts the [intent]
 */
fun Controller.startActivity(intent: Intent) {
    activity.startActivity(intent)
}

/**
 * Returns a new router transaction
 */
fun Controller.toTransaction(): RouterTransaction = RouterTransaction(this)

/**
 * Returns the child router for [container] and [tag] or creates a new instance
 */
fun Controller.getChildRouter(
    container: ViewGroup,
    tag: String? = null,
    controllerFactory: ControllerFactory? = null
): Router = getChildRouter(container.id, tag, controllerFactory)