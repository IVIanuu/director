package com.ivianuu.director

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.internal.ControllerHostedRouter
import com.ivianuu.director.internal.ViewAttachHandler
import java.lang.ref.WeakReference
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
        get() = if (routerSet) _router else throw IllegalStateException("router is only available after onCreate")

    private lateinit var _router: Router
    private var routerSet = false

    /**
     * Objects which will retained across configuration changes
     */
    val retainedObjects get() = if (routerSet) _retainedObjects else throw IllegalStateException("retainedObjects is only available after onCreate")
    private lateinit var _retainedObjects: RetainedObjects

    /**
     * Returns the host activity of this controller
     */
    val activity: FragmentActivity
        get() = if (routerSet) router.activity else throw IllegalStateException("activity is only available after onCreate")

    /**
     * The view of this controller or null
     */
    var view: View? = null
        private set

    /**
     * The parent controller of this controller or null
     */
    var parentController: Controller? = null
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
        get() = targetInstanceId?.let { _router.rootRouter.findControllerByInstanceId(it) }
        set(value) {
            if (targetInstanceId != null) {
                throw IllegalStateException("the target controller can only be set once")
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
        internal set

    private var allState: Bundle? = null
    private var instanceState: Bundle? = null
    private var viewState: Bundle? = null
    private var childRouterStates: Map<ControllerHostedRouter, Bundle>? = null

    private var viewIsAttached = false
    private var attachedToUnownedParent = false
    private var awaitingParentAttach = false
    private var hasSavedViewState = false

    /**
     * Overrides the push handler which will be used when this controller gets pushed
     */
    var overriddenPushHandler: ControllerChangeHandler? = null

    /**
     * Overrides the pop handler which will be used when this controller gets popped
     */
    var overriddenPopHandler: ControllerChangeHandler? = null

    /**
     * Whether or not the view should be retained while being detached
     */
    var retainView = false
        set(value) {
            field = value
            if (!value && !isAttached) {
                removeViewReference(false)
            }
        }

    private var viewAttachHandler: ViewAttachHandler? = null

    /**
     * All child routers of this controller
     */
    val childRouters: List<Router>
        get() = _childRouters.toList()
    private val _childRouters = mutableListOf<ControllerHostedRouter>()

    private val lifecycleListeners = mutableSetOf<ControllerLifecycleListener>()

    private var destroyedView: WeakReference<View>? = null

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
     * Called when this Controller has been destroyed.
     */
    protected open fun onDestroy() {
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
     * Called when the view of this controller gets destroyed
     */
    protected open fun onUnbindView(view: View) {
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
     * Called to save the view state of this controller
     */
    protected open fun onSaveViewState(view: View, outState: Bundle) {
        superCalled = true
    }

    /**
     * Restores the view state which was saved in [onSaveViewState]
     */
    protected open fun onRestoreViewState(view: View, savedViewState: Bundle) {
        superCalled = true
    }

    /**
     * Called to save the instance state of this controller
     */
    protected open fun onSaveInstanceState(outState: Bundle) {
        superCalled = true
    }

    /**
     * Restores the instance state which was saved in [onSaveInstanceState]
     */
    protected open fun onRestoreInstanceState(savedInstanceState: Bundle) {
        superCalled = true
    }

    /**
     * Should be overridden if this Controller has called startActivityForResult and needs to handle
     * the result.
     */
    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    /**
     * Should be overridden if this Controller has requested runtime permissions and needs to handle the user's response.
     */
    open fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
    }

    /**
     * Gets whether you should show UI with rationale for requesting a permission.
     */
    open fun shouldShowRequestPermissionRationale(permission: String) =
        Build.VERSION.SDK_INT >= 23
                && activity.shouldShowRequestPermissionRationale(permission)

    /**
     * Should be overridden if this Controller needs to handle the back button being pressed.
     */
    open fun handleBack() = _childRouters
        .flatMap { it.backstack }
        .asSequence()
        .sortedByDescending { it.transactionIndex }
        .any { it.controller.isAttached && it.controller.router.handleBack() }

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
     * Returns the child router for [containerId] and [tag] or creates a new instance
     */
    fun getChildRouter(containerId: Int, tag: String? = null): Router =
        getChildRouter(containerId, tag, true)!!

    /**
     * Returns the child router for [containerId] and [tag] if already created
     */
    fun getChildRouterOrNull(containerId: Int, tag: String? = null): Router? =
        getChildRouter(containerId, tag, false)

    private fun getChildRouter(
        containerId: Int,
        tag: String?,
        createIfNeeded: Boolean
    ): ControllerHostedRouter? {
        var childRouter = _childRouters
            .firstOrNull { it.hostId == containerId && it.tag == tag }

        if (childRouter == null) {
            if (createIfNeeded) {
                childRouter = ControllerHostedRouter(
                    this,
                    containerId,
                    tag
                )

                _childRouters.add(childRouter)
            }
        }

        return childRouter.also { restoreChildControllerContainers() }
    }

    /**
     * Removes the [childRouter]. All Controllers currently managed by
     * the [childRouter] will be destroyed.
     */
    fun removeChildRouter(childRouter: Router) {
        if (childRouter is ControllerHostedRouter && _childRouters.remove(childRouter)) {
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
        if (routerSet) {
            throw IllegalStateException("controller already added")
        }

        if (state != null) {
            val className = state.getString(KEY_CLASS_NAME)
            if (javaClass.name != className) {
                throw IllegalArgumentException("state of $className cannot be used for ${javaClass.name}")
            }
        }

        allState = state
    }

    internal fun setRouter(router: Router) {
        if (routerSet) return
        routerSet = true
        _router = router

        // restore the internal state
        allState?.let { restoreInstanceState(it) }
        allState = null

        // get the retained objects back
        _retainedObjects = router.getRetainedObjects(instanceId)

        // create
        performCreate()

        // restore instance state
        performRestoreInstanceState()
    }

    internal fun prepareForHostDetach() {
        _childRouters.forEach { it.prepareForHostDetach() }
    }

    internal fun activityStarted() {
        viewAttachHandler?.onActivityStarted()
        _childRouters.forEach { it.onActivityStarted() }
    }

    internal fun activityResumed() {
        val view = view
        if (!isAttached && view != null && viewIsAttached) {
            attach(view)
        } else if (isAttached) {
            hasSavedViewState = false
        }

        _childRouters.forEach { it.onActivityResumed() }
    }

    internal fun activityPaused() {
        _childRouters.forEach { it.onActivityPaused() }
    }

    internal fun activityStopped() {
        viewAttachHandler?.onActivityStopped()

        // cancel any pending input event
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view?.cancelPendingInputEvents()
        }

        _childRouters.forEach { it.onActivityStopped() }
    }

    internal fun activityDestroyed() {
        destroy(true)
        _childRouters.forEach { it.onActivityDestroyed() }
    }

    internal fun inflate(parent: ViewGroup): View {
        var view = view

        if (view != null && view.parent != null && view.parent != parent) {
            detach(view, true, false, false, false)
            removeViewReference(false)
            view = null
        }

        if (view == null) {
            notifyLifecycleListeners { it.preInflateView(this, viewState) }

            view = onInflateView(
                LayoutInflater.from(parent.context),
                parent,
                viewState
            ).also { this.view = it }

            restoreChildControllerContainers()

            notifyLifecycleListeners { it.postInflateView(this, view, viewState) }

            notifyLifecycleListeners { it.preBindView(this, view, viewState) }

            requireSuperCalled { onBindView(view, viewState) }

            notifyLifecycleListeners { it.postBindView(this, view, viewState) }

            restoreViewState(view)

            viewAttachHandler = ViewAttachHandler(object : ViewAttachHandler.Listener {
                override fun onAttached() {
                    viewIsAttached = true
                    attach(view)
                }

                override fun onDetached(fromActivityStop: Boolean) {
                    viewIsAttached = false
                    detach(view, false, fromActivityStop, false, false)
                }

                override fun onViewDetachAfterStop() {
                    detach(view, false, false, false, false)
                }
            }).also { it.listenForAttach(view) }
        } else if (retainView) {
            restoreChildControllerContainers()
        }

        return view
    }

    private fun attach(view: View) {
        val router = router

        attachedToUnownedParent = view.parent != router.container

        // this can happen while transitions just ignore it
        if (attachedToUnownedParent) return

        // do not attach while destroyed
        if (isBeingDestroyed) return

        val parentController = parentController

        if (parentController != null && !parentController.isAttached) {
            awaitingParentAttach = true
            return
        } else {
            awaitingParentAttach = false
        }

        hasSavedViewState = false

        notifyLifecycleListeners { it.preAttach(this, view) }

        isAttached = true

        requireSuperCalled { onAttach(view) }

        notifyLifecycleListeners { it.postAttach(this, view) }

        _childRouters
            .flatMap { it.backstack }
            .filter { it.controller.awaitingParentAttach }
            .forEach { it.controller.attach(it.controller.view!!) }
    }

    internal fun detach(
        view: View,
        forceViewRemoval: Boolean,
        blockViewRemoval: Boolean,
        forceChildViewRemoval: Boolean,
        fromHostRemoval: Boolean
    ) {
        if (!attachedToUnownedParent) {
            _childRouters.forEach { it.prepareForHostDetach() }
        }

        val removeViewRef =
            !blockViewRemoval && (forceViewRemoval || !retainView || isBeingDestroyed)

        if (isAttached) {
            notifyLifecycleListeners { it.preDetach(this, view) }
            isAttached = false

            requireSuperCalled { onDetach(view) }

            notifyLifecycleListeners { it.postDetach(this, view) }
        }

        if (removeViewRef) {
            removeViewReference(forceChildViewRemoval)
        } else if (retainView && fromHostRemoval) {
            // this happens if we are a child controller, have RETAIN_DETACH
            // and the parent does not RETAIN_DETACH
            // we remove the view from the container to make sure that
            // we don't re-inflate the view in Controller.inflate
            // because we would be attached to the old container otherwise
            (view.parent as? ViewGroup)?.removeView(view)
        }
    }

    private fun removeViewReference(forceChildViewRemoval: Boolean) {
        val view = view
        if (view != null) {
            if (!isBeingDestroyed && !hasSavedViewState) {
                saveViewState(view)
            }

            notifyLifecycleListeners { it.preUnbindView(this, view) }

            requireSuperCalled { onUnbindView(view) }

            viewAttachHandler?.unregisterAttachListener(view)
            viewAttachHandler = null
            viewIsAttached = false

            if (isBeingDestroyed) {
                destroyedView = WeakReference(view)
            }

            this.view = null

            notifyLifecycleListeners { it.postUnbindView(this) }

            _childRouters.forEach { it.removeContainer(forceChildViewRemoval) }
        }

        if (isBeingDestroyed) {
            performDestroy()
        }
    }

    private fun restoreChildControllerContainers() {
        _childRouters
            .filterNot { it.hasContainer }
            .forEach {
                val containerView = (view?.findViewById(it.hostId) as? ViewGroup) ?: return@forEach
                it.container = containerView
                it.rebind()
            }
    }

    internal fun destroy() {
        destroy(false)
    }

    private fun destroy(removeViews: Boolean) {
        isBeingDestroyed = true

        _childRouters.forEach { it.destroy(false) }

        router.unregisterForActivityResults(instanceId)

        if (!isAttached) {
            removeViewReference(true)
        } else if (removeViews) {
            view?.let {
                detach(
                    it,
                    true,
                    false,
                    true,
                    false
                )
            }
        }

        if (!activity.isChangingConfigurations) {
            router.removeRetainedObjects(instanceId)
            _retainedObjects.clear()
        }
    }

    private fun performDestroy() {
        if (isDestroyed) return

        notifyLifecycleListeners { it.preDestroy(this) }
        isDestroyed = true

        requireSuperCalled { onDestroy() }

        parentController = null
        notifyLifecycleListeners { it.postDestroy(this) }
    }

    internal fun saveInstanceState(): Bundle {
        val view = view
        if (!hasSavedViewState && view != null) {
            saveViewState(view)
        }

        val outState = Bundle()
        outState.putString(KEY_CLASS_NAME, javaClass.name)
        outState.putBundle(KEY_VIEW_STATE, viewState)
        outState.putBundle(KEY_ARGS, args)
        outState.putString(KEY_INSTANCE_ID, instanceId)
        outState.putString(KEY_TARGET_INSTANCE_ID, instanceId)
        outState.putBoolean(KEY_RETAIN_VIEW, retainView)

        overriddenPushHandler?.let {
            outState.putBundle(
                KEY_OVERRIDDEN_PUSH_HANDLER,
                it.toBundle()
            )
        }
        overriddenPopHandler?.let { outState.putBundle(KEY_OVERRIDDEN_POP_HANDLER, it.toBundle()) }

        val childBundles = _childRouters
            .map { childRouter ->
                Bundle().also {
                    childRouter.saveIdentity(it)
                    childRouter.saveInstanceState(it)
                }
            }
        outState.putParcelableArrayList(KEY_CHILD_ROUTERS, ArrayList(childBundles))

        val savedState = Bundle(javaClass.classLoader)
        requireSuperCalled { onSaveInstanceState(savedState) }

        notifyLifecycleListeners { it.onSaveInstanceState(this, savedState) }

        outState.putBundle(KEY_SAVED_STATE, savedState)

        return outState
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.getBundle(KEY_ARGS)?.let { bundle ->
            args = bundle.apply { classLoader = this@Controller.javaClass.classLoader }
        }

        instanceState = savedInstanceState.getBundle(KEY_SAVED_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        viewState = savedInstanceState.getBundle(KEY_VIEW_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        instanceId = savedInstanceState.getString(KEY_INSTANCE_ID)!!
        targetInstanceId = savedInstanceState.getString(KEY_TARGET_INSTANCE_ID)

        overriddenPushHandler = savedInstanceState.getBundle(KEY_OVERRIDDEN_PUSH_HANDLER)
            ?.let { ControllerChangeHandler.fromBundle(it) }
        overriddenPopHandler = savedInstanceState.getBundle(KEY_OVERRIDDEN_POP_HANDLER)
            ?.let { ControllerChangeHandler.fromBundle(it) }

        retainView = savedInstanceState.getBoolean(KEY_RETAIN_VIEW)

        childRouterStates = savedInstanceState.getParcelableArrayList<Bundle>(KEY_CHILD_ROUTERS)!!
            .map { bundle ->
                ControllerHostedRouter(this).apply {
                    // we only restore the identity for now
                    // to give the user a chance to set a [ControllerFactory] in [onCreate]
                    restoreIdentity(bundle)
                } to bundle
            }
            .onEach { _childRouters.add(it.first) }
            .toMap()
    }

    private fun saveViewState(view: View) {
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

    private fun restoreViewState(view: View) {
        val viewState = viewState ?: return

        view.restoreHierarchyState(viewState.getSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY))
        val savedViewState = viewState.getBundle(KEY_VIEW_STATE_BUNDLE)!!
        savedViewState.classLoader = javaClass.classLoader
        onRestoreViewState(view, savedViewState)

        restoreChildControllerContainers()

        notifyLifecycleListeners { it.onRestoreViewState(this, viewState) }
    }

    private fun performCreate() {
        if (isCreated) return
        notifyLifecycleListeners { it.preCreate(this, instanceState) }

        isCreated = true

        requireSuperCalled { onCreate(instanceState) }

        notifyLifecycleListeners { it.postCreate(this, instanceState) }
    }

    private fun performRestoreInstanceState() {
        val state = instanceState ?: return
        onRestoreInstanceState(state)
        notifyLifecycleListeners { it.onRestoreInstanceState(this, state) }
        instanceState = null
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

        val destroyedView = destroyedView?.get()

        if (isBeingDestroyed && !viewIsAttached && !isAttached && destroyedView != null) {
            val router = router

            val container = router.container
            if (container != null && destroyedView.parent == router.container) {
                container.removeView(destroyedView)
            }

            this.destroyedView = null
        }
    }

    private inline fun notifyLifecycleListeners(block: (ControllerLifecycleListener) -> Unit) {
        val listeners = lifecycleListeners + router.getAllLifecycleListeners()
        listeners.forEach(block)
    }

    private inline fun requireSuperCalled(block: () -> Unit) {
        superCalled = false
        block()
        if (!superCalled) {
            throw IllegalStateException("super not called ${javaClass.name}")
        }
    }

    companion object {
        private const val KEY_CLASS_NAME = "Controller.className"
        private const val KEY_VIEW_STATE = "Controller.viewState"
        private const val KEY_CHILD_ROUTERS = "Controller.childRouters"
        private const val KEY_SAVED_STATE = "Controller.instanceState"
        private const val KEY_INSTANCE_ID = "Controller.instanceId"
        private const val KEY_TARGET_INSTANCE_ID = "Controller.targetInstanceId"
        private const val KEY_ARGS = "Controller.args"
        private const val KEY_OVERRIDDEN_PUSH_HANDLER = "Controller.overriddenPushHandler"
        private const val KEY_OVERRIDDEN_POP_HANDLER = "Controller.overriddenPopHandler"
        private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
        private const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"
        private const val KEY_RETAIN_VIEW = "Controller.retainViewMode"

        internal fun fromBundle(bundle: Bundle, factory: ControllerFactory): Controller {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            val args = bundle.getBundle(KEY_ARGS)
            return factory.instantiateController(
                Controller::class.java.classLoader!!, className, args!!
            ).apply {
                allState = bundle
            }
        }
    }
}