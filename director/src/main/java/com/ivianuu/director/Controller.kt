package com.ivianuu.director

import android.annotation.TargetApi
import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
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
import com.ivianuu.director.internal.d
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
    val retainedObjects get() = _retainedObjects

    private lateinit var _retainedObjects: RetainedObjects

    /**
     * Returns the host activity of this controller
     */
    val activity: FragmentActivity
        get() = router.activity

    /**
     * The application of the [activity]
     */
    val application: Application
        get() = activity.application

    /**
     * The [activity]s resources
     */
    val resources: Resources get() = activity.resources

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
        set(value) { targetInstanceId = value?.instanceId }

    private var targetInstanceId: String? = null

    /**
     * Whether or not this controller is already created
     */
    var isCreated = false
        private set

    /**
     * Whether or not this Controller is currently attached to a host View.
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
    private var savedState: Bundle? = null
    private var viewState: Bundle? = null
    private var childRouterStates: Map<ControllerHostedRouter, Bundle>? = null

    internal var needsAttach = false
    private var viewIsAttached = false
    private var viewWasDetached = false
    private var attachedToUnownedParent = false
    private var awaitingParentAttach = false
    private var hasSavedViewState = false

    internal var isDetachFrozen = false
        set(value) {
            if (field != value) {
                field = value
                _childRouters.forEach { it.isDetachFrozen = value }

                val view = view
                if (!value && view != null && viewWasDetached) {
                    detach(view, false, false, false, false)
                }
            }
        }

    /**
     * Overrides the push handler which will be used when this controller gets pushed
     */
    var overriddenPushHandler: ControllerChangeHandler? = null

    /**
     * Overrides the pop handler which will be used when this controller gets popped
     */
    var overriddenPopHandler: ControllerChangeHandler? = null

    /**
     * Whether or not the view should be retained
     */
    var retainViewMode = RetainViewMode.RELEASE_DETACH
        set(value) {
            field = value
            if (value == RetainViewMode.RELEASE_DETACH && !isAttached) {
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
    private val requestedPermissions = mutableListOf<String>()

    private var destroyedView: WeakReference<View>? = null
    private var isPerformingExitTransition = false

    private var superCalled = false

    /**
     * Will be called once when the router was set for the first time
     */
    protected open fun onCreate() {
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
     * Called when the controller is ready to display its view. A valid view must be returned. The standard body
     * for this method will be `return inflater.inflate(R.layout.my_layout, container, false);`, plus
     * any binding code.
     */
    protected abstract fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View

    /**
     * Called when this controllers view was created
     */
    protected open fun onBindView(view: View) {
        superCalled = true
    }

    /**
     * Called when this Controller's View is being destroyed. This should overridden to unbind the View
     * from any local variables.
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
     * Called when this Controller has a Context available to it. This will happen very early on in the lifecycle
     * (before a view is created). If the host activity is re-created (ex: for orientation change), this will be
     * called again when the new context is available.
     */
    protected open fun onActivitySet(activity: FragmentActivity) {
        superCalled = true
    }

    /**
     * Called when this Controller's Context is no longer available. This can happen when the Controller is
     * destroyed or when the host Activity is destroyed.
     */
    protected open fun onActivityCleared() {
        superCalled = true
    }

    /**
     * Called when this Controller is attached to its host ViewGroup
     */
    protected open fun onAttach(view: View) {
        superCalled = true
    }

    /**
     * Called when this Controller is detached from its host ViewGroup
     */
    protected open fun onDetach(view: View) {
        superCalled = true
    }

    /**
     * Called to save this Controller's View state. As Views can be detached and destroyed as part of the
     * Controller lifecycle (ex: when another Controller has been pushed on top of it), care should be taken
     * to save anything needed to reconstruct the View.
     */
    protected open fun onSaveViewState(view: View, outState: Bundle) {
        superCalled = true
    }

    /**
     * Restores data that was saved in the [.onSaveViewState] method. This should be overridden
     * to restore the View's state to where it was before it was destroyed.
     */
    protected open fun onRestoreViewState(view: View, savedViewState: Bundle) {
        superCalled = true
    }

    /**
     * Called to save this Controller's state in the event that its host Activity is destroyed.
     */
    protected open fun onSaveInstanceState(outState: Bundle) {
        superCalled = true
    }

    /**
     * Restores data that was saved in the [.onSaveInstanceState] method. This should be overridden
     * to restore this Controller's state to where it was before it was destroyed.
     */
    protected open fun onRestoreInstanceState(savedInstanceState: Bundle) {
        superCalled = true
    }

    /**
     * Calls startActivity(Intent) from this Controller's host Activity.
     */
    fun startActivity(intent: Intent) {
        router.startActivity(intent)
    }

    /**
     * Calls startActivityForResult(Intent, int) from this Controller's host Activity.
     */
    fun startActivityForResult(intent: Intent, requestCode: Int) {
        router.startActivityForResult(instanceId, intent, requestCode)
    }

    /**
     * Calls startActivityForResult(Intent, int, Bundle) from this Controller's host Activity.
     */
    fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        router.startActivityForResult(instanceId, intent, requestCode, options)
    }

    /**
     * Calls startIntentSenderForResult(IntentSender, int, Intent, int, int, int, Bundle) from this Controller's host Activity.
     */
    fun startIntentSenderForResult(
        intent: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int,
        flagsValues: Int, extraFlags: Int, options: Bundle?
    ) {
        router.startIntentSenderForResult(
            instanceId,
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
            options
        )
    }

    /**
     * Registers this Controller to handle onActivityResult responses. Calling this method is NOT
     * necessary when calling [.startActivityForResult]
     */
    fun registerForActivityResult(requestCode: Int) {
        router.registerForActivityResult(instanceId, requestCode)
    }

    /**
     * Should be overridden if this Controller has called startActivityForResult and needs to handle
     * the result.
     */
    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

    /**
     * Calls requestPermission(String[], int) from this Controller's host Activity. Results for this request,
     * including [.shouldShowRequestPermissionRationale] and
     * [.onRequestPermissionsResult] will be forwarded back to this Controller by the system.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissions(permissions: Array<String>, requestCode: Int) {
        requestedPermissions.addAll(Arrays.asList(*permissions))
        router.requestPermissions(instanceId, permissions, requestCode)
    }

    /**
     * Gets whether you should show UI with rationale for requesting a permission.
     * {@see android.app.Activity#shouldShowRequestPermissionRationale(String)}
     */
    open fun shouldShowRequestPermissionRationale(permission: String) =
        Build.VERSION.SDK_INT >= 23
                && activity.shouldShowRequestPermissionRationale(permission)

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
     * Returns the child router for [container] and [tag] or creates a new instance
     */
    fun getChildRouter(container: ViewGroup, tag: String? = null) =
        getChildRouter(container.id, tag)

    /**
     * Returns the child router for [container] and [tag] if already created
     */
    fun getChildRouterOrNull(container: ViewGroup, tag: String? = null) =
        getChildRouter(container.id, tag)

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

                if (isPerformingExitTransition) {
                    childRouter.isDetachFrozen = true
                }
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
        needsAttach = needsAttach || isAttached
        _childRouters.forEach { it.prepareForHostDetach() }
    }

    internal fun didRequestPermission(permission: String) =
        requestedPermissions.contains(permission)

    internal fun requestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        requestedPermissions.removeAll(Arrays.asList(*permissions))
        onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    internal fun activityStarted(activity: FragmentActivity) {
        viewAttachHandler?.onActivityStarted()
    }

    internal fun activityResumed(activity: FragmentActivity) {
        val view = view
        if (!isAttached && view != null && viewIsAttached) {
            attach(view)
        } else if (isAttached) {
            needsAttach = false
            hasSavedViewState = false
        }
    }

    internal fun activityPaused(activity: FragmentActivity) {
    }

    internal fun activityStopped(activity: FragmentActivity) {
        viewAttachHandler?.onActivityStopped()

        // todo check this
        if (isAttached && activity.isChangingConfigurations) {
            d { "is attached and changing configs" }
            needsAttach = true
        }
    }

    internal fun activityDestroyed(activity: FragmentActivity) {
        destroy(true)
    }

    internal fun inflate(parent: ViewGroup): View {
        var view = view

        if (view != null && view.parent != null && view.parent != parent) {
            detach(view, true, false, false, false)
            removeViewReference(false)
            view = null
        }

        if (view == null) {
            notifyLifecycleListeners { it.preInflateView(this) }

            view = onInflateView(
                LayoutInflater.from(parent.context),
                parent,
                viewState
            ).also { this.view = it }

            restoreChildControllerContainers()

            notifyLifecycleListeners { it.postInflateView(this, view) }

            notifyLifecycleListeners { it.preBindView(this, view) }

            requireSuperCalled { onBindView(view) }

            notifyLifecycleListeners { it.postBindView(this, view) }

            restoreViewState(view)

            viewAttachHandler = ViewAttachHandler(object : ViewAttachHandler.Listener {
                override fun onAttached() {
                    viewIsAttached = true
                    viewWasDetached = false
                    attach(view)
                }

                override fun onDetached(fromActivityStop: Boolean) {
                    viewIsAttached = false
                    viewWasDetached = true

                    if (!isDetachFrozen) {
                        detach(view, false, fromActivityStop, false, false)
                    }
                }

                override fun onViewDetachAfterStop() {
                    if (!isDetachFrozen) {
                        detach(view, false, false, false, false)
                    }
                }
            }).also { it.listenForAttach(view) }
        } else if (retainViewMode == RetainViewMode.RETAIN_DETACH) {
            restoreChildControllerContainers()
        }

        return view
    }

    internal fun attach(view: View) {
        val router = router

        attachedToUnownedParent = view.parent != router.container

        // this can happen while transitions just ignore it
        if (attachedToUnownedParent) {
            return
        }

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
        needsAttach = router.isActivityStopped == true

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
            !blockViewRemoval && (forceViewRemoval || retainViewMode == RetainViewMode.RELEASE_DETACH || isBeingDestroyed)

        if (isAttached) {
            notifyLifecycleListeners { it.preDetach(this, view) }
            isAttached = false

            if (!awaitingParentAttach) {
                requireSuperCalled { onDetach(view) }
            }

            notifyLifecycleListeners { it.postDetach(this, view) }
        }

        if (removeViewRef) {
            removeViewReference(forceChildViewRemoval)
        } else if (retainViewMode == RetainViewMode.RETAIN_DETACH && fromHostRemoval) {
            // this happens if we are a child controller, have RETAIN_DETACH
            // and the parent does not RETAIN_DETACH
            // we remove the view from the container to make sure that
            // we dont reinflate the view in Controller.inflate
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
                val containerView = view?.findViewById(it.hostId) as? ViewGroup

                if (containerView != null) {
                    it.container = containerView
                    it.rebindIfNeeded()
                }
            }
    }

    private fun performDestroy() {
        if (!isDestroyed) {
            notifyLifecycleListeners { it.preDestroy(this) }
            isDestroyed = true

            requireSuperCalled { onDestroy() }

            parentController = null
            notifyLifecycleListeners { it.postDestroy(this) }
        }
    }

    internal fun destroy() {
        destroy(false)
    }

    private fun destroy(removeViews: Boolean) {
        isBeingDestroyed = true

        router.unregisterForActivityResults(instanceId)

        _childRouters.forEach { it.destroy(false) }

        if (!isAttached) {
            removeViewReference(true)
        } else if (removeViews) {
            view?.let { detach(it, true, false, true, false) }
        }
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
        outState.putBoolean(KEY_NEEDS_ATTACH, needsAttach || isAttached)
        outState.putInt(KEY_RETAIN_VIEW_MODE, retainViewMode.ordinal)

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
                    childRouter.saveBasicInstanceState(it)
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

        savedState = savedInstanceState.getBundle(KEY_SAVED_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        viewState = savedInstanceState.getBundle(KEY_VIEW_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        instanceId = savedInstanceState.getString(KEY_INSTANCE_ID)!!
        targetInstanceId = savedInstanceState.getString(KEY_TARGET_INSTANCE_ID)

        overriddenPushHandler = savedInstanceState.getBundle(KEY_OVERRIDDEN_PUSH_HANDLER)
            ?.let { ControllerChangeHandler.fromBundle(it) }
        overriddenPopHandler = savedInstanceState.getBundle(KEY_OVERRIDDEN_POP_HANDLER)
            ?.let { ControllerChangeHandler.fromBundle(it) }

        needsAttach = savedInstanceState.getBoolean(KEY_NEEDS_ATTACH)

        retainViewMode = RetainViewMode.values()[savedInstanceState.getInt(KEY_RETAIN_VIEW_MODE, 0)]

        childRouterStates = savedInstanceState.getParcelableArrayList<Bundle>(KEY_CHILD_ROUTERS)!!
            .map { bundle ->
                ControllerHostedRouter(this).apply {
                    // we do not restore the full instance yet
                    // to give the user a chance to set a [ControllerFactory]
                    restoreBasicInstanceState(bundle)
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
        val viewState = viewState
        if (viewState != null) {
            view.restoreHierarchyState(viewState.getSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY))
            val savedViewState = viewState.getBundle(KEY_VIEW_STATE_BUNDLE)!!
            savedViewState.classLoader = javaClass.classLoader
            onRestoreViewState(view, savedViewState)

            restoreChildControllerContainers()

            notifyLifecycleListeners { it.onRestoreViewState(this, viewState) }
        }
    }

    private fun performCreate() {
        if (!isCreated) {
            notifyLifecycleListeners { it.preCreate(this) }

            superCalled = false

            onCreate()

            if (!superCalled) {
                throw IllegalStateException("${javaClass.name} did not call super.onCreate()")
            }

            isCreated = true
            notifyLifecycleListeners { it.postCreate(this) }
        }
    }

    private fun performRestoreInstanceState() {
        val savedInstanceState = savedState
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
            notifyLifecycleListeners { it.onRestoreInstanceState(this, savedInstanceState) }
            savedState = null
        }
    }

    internal fun changeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        if (!changeType.isEnter) {
            isPerformingExitTransition = true
            _childRouters.forEach { it.isDetachFrozen = true }
        }

        onChangeStarted(changeHandler, changeType)

        notifyLifecycleListeners { it.onChangeStart(this, changeHandler, changeType) }
    }

    internal fun changeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        if (!changeType.isEnter) {
            isPerformingExitTransition = false
            _childRouters.forEach { it.isDetachFrozen = false }
        }

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
        val listeners = mutableListOf<ControllerLifecycleListener>()
        listeners.addAll(lifecycleListeners)
        listeners.addAll(router.getAllLifecycleListeners())
        listeners.forEach(block)
    }

    private inline fun requireSuperCalled(action: () -> Unit) {
        superCalled = false
        action()
        if (!superCalled) {
            throw IllegalStateException("super not called ${javaClass.name}")
        }
    }

    /** Modes that will influence when the Controller will allow its view to be destroyed  */
    enum class RetainViewMode {
        /** The Controller will release its reference to its view as soon as it is detached.  */
        RELEASE_DETACH,
        /** The Controller will retain its reference to its view when detached, but will still release the reference when a config change occurs.  */
        RETAIN_DETACH
    }

    companion object {
        private const val KEY_CLASS_NAME = "Controller.className"
        private const val KEY_VIEW_STATE = "Controller.viewState"
        private const val KEY_CHILD_ROUTERS = "Controller.childRouters"
        private const val KEY_SAVED_STATE = "Controller.savedState"
        private const val KEY_INSTANCE_ID = "Controller.instanceId"
        private const val KEY_TARGET_INSTANCE_ID = "Controller.targetInstanceId"
        private const val KEY_ARGS = "Controller.args"
        private const val KEY_NEEDS_ATTACH = "Controller.needsAttach"
        private const val KEY_OVERRIDDEN_PUSH_HANDLER = "Controller.overriddenPushHandler"
        private const val KEY_OVERRIDDEN_POP_HANDLER = "Controller.overriddenPopHandler"
        private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
        private const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"
        private const val KEY_RETAIN_VIEW_MODE = "Controller.retainViewMode"

        internal fun fromBundle(bundle: Bundle, factory: ControllerFactory): Controller {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            val args = bundle.getBundle(KEY_ARGS)
            return factory.instantiateController(javaClass.classLoader, className, args!!).apply {
                allState = bundle
            }
        }
    }
}