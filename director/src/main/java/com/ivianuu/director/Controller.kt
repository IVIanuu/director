package com.ivianuu.director

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.internal.ControllerHostedRouter
import com.ivianuu.director.internal.ViewAttachHandler
import com.ivianuu.director.internal.ViewAttachHandler.ViewAttachListener
import com.ivianuu.director.internal.classForNameOrThrow
import com.ivianuu.director.internal.newInstanceOrThrow
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
     * Returns any arguments that were set in this Controller's constructor
     */
    var args = Bundle(javaClass.classLoader)

    /**
     * Returns the router of this controller
     */
    var router: Router? = null
        internal set(value) {
            field = value
            performCreate()

            if (value != null) {
                onRouterSetListeners.forEach { it.invoke(value) }
                onRouterSetListeners.clear()
            }
        }

    /**
     * Return this Controller's View or `null` if it has not yet been created or has been
     * destroyed.
     */
    var view: View? = null
        private set

    /**
     * Returns this Controller's parent Controller if it is a child Controller or `null` if
     * it has no parent.
     */
    var parentController: Controller? = null
        internal set

    /**
     * Returns this Controller's instance ID, which is generated when the instance is created and
     * retained across restarts.
     */
    var instanceId = UUID.randomUUID().toString()
        private set

    private var targetInstanceId: String? = null

    var targetController: Controller?
        get() = targetInstanceId?.let { router?.rootRouter?.findControllerByInstanceId(it) }
        set(value) { targetInstanceId = value?.instanceId }

    /**
     * Returns whether or not this controller is already created
     */
    var isCreated = false
        private set

    /**
     * Returns whether or not this Controller is currently attached to a host View.
     */
    var isAttached = false
        private set

    /**
     * Returns whether or not this Controller has been destroyed.
     */
    var isDestroyed = false
        private set

    /**
     * Returns whether or not this Controller is currently in the process of being destroyed.
     */
    var isBeingDestroyed = false
        internal set

    /**
     * Whether or not this controller has an options menu
     */
    var hasOptionsMenu = false
        set(value) {
            val invalidate = isAttached && !optionsMenuHidden && field != value
            field = value
            if (invalidate) withRouter { it.invalidateOptionsMenu() }
        }

    /**
     * Whether or not the options menu of this controller should be hidden
     */
    var optionsMenuHidden = false
        set(value) {
            val invalidate = isAttached && hasOptionsMenu && field != value
            field = value
            if (invalidate) withRouter { it.invalidateOptionsMenu() }
        }

    private var viewState: Bundle? = null
    private var savedInstanceState: Bundle? = null

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
                onChildRouters { it.isDetachFrozen = value }

                val view = view
                if (!value && view != null && viewWasDetached) {
                    detach(view, false, false, false)
                }
            }
        }

    /**
     * Overrides the push handler which will be used when this controller gets pushed
     */
    var overriddenPushHandler: ControllerChangeHandler? = null

    /**
     * Overries the pop handler which will be used when this controller gets popped
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
     * Returns all of this Controller's child Routers
     */
    val childRouters: List<Router>
        get() = _childRouters.toList()
    private val _childRouters = mutableListOf<ControllerHostedRouter>()

    private val lifecycleListeners = mutableSetOf<ControllerLifecycleListener>()
    private val requestedPermissions = mutableListOf<String>()
    private val onRouterSetListeners = mutableListOf<((Router) -> Unit)>()
    private var destroyedView: WeakReference<View>? = null
    private var isPerformingExitTransition = false
    private var isContextAvailable = false

    /**
     * Returns the host activity of this controller
     */
    val activity: Activity?
        get() = router?.activity

    /**
     * Will be called once when the router was set for the first time
     */
    protected open fun onCreate() {
    }

    /**
     * Called when this Controller has been destroyed.
     */
    protected open fun onDestroy() {
    }

    /**
     * Called when the controller is ready to display its view. A valid view must be returned. The standard body
     * for this method will be `return inflater.inflate(R.layout.my_layout, container, false);`, plus
     * any binding code.
     */
    protected abstract fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View

    /**
     * Called when this Controller's View is being destroyed. This should overridden to unbind the View
     * from any local variables.
     */
    protected open fun onDestroyView(view: View) {
    }

    /**
     * Called when this Controller begins the process of being swapped in or out of the host view.
     */
    protected open fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
    }

    /**
     * Called when this Controller completes the process of being swapped in or out of the host view.
     */
    protected open fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
    }

    /**
     * Called when this Controller has a Context available to it. This will happen very early on in the lifecycle
     * (before a view is created). If the host activity is re-created (ex: for orientation change), this will be
     * called again when the new context is available.
     */
    protected open fun onContextAvailable(context: Context) {
    }

    /**
     * Called when this Controller's Context is no longer available. This can happen when the Controller is
     * destroyed or when the host Activity is destroyed.
     */
    protected open fun onContextUnavailable() {
    }

    /**
     * Called when this Controller is attached to its host ViewGroup
     */
    protected open fun onAttach(view: View) {
    }

    /**
     * Called when this Controller is detached from its host ViewGroup
     */
    protected open fun onDetach(view: View) {
    }

    /**
     * Called when this Controller's host Activity is started
     */
    protected open fun onActivityStarted(activity: Activity) {
    }

    /**
     * Called when this Controller's host Activity is resumed
     */
    protected open fun onActivityResumed(activity: Activity) {
    }

    /**
     * Called when this Controller's host Activity is paused
     */
    protected open fun onActivityPaused(activity: Activity) {
    }

    /**
     * Called when this Controller's host Activity is stopped
     */
    protected open fun onActivityStopped(activity: Activity) {
    }

    /**
     * Called to save this Controller's View state. As Views can be detached and destroyed as part of the
     * Controller lifecycle (ex: when another Controller has been pushed on top of it), care should be taken
     * to save anything needed to reconstruct the View.
     */
    protected open fun onSaveViewState(view: View, outState: Bundle) {
    }

    /**
     * Restores data that was saved in the [.onSaveViewState] method. This should be overridden
     * to restore the View's state to where it was before it was destroyed.
     */
    protected open fun onRestoreViewState(view: View, savedViewState: Bundle) {
    }

    /**
     * Called to save this Controller's state in the event that its host Activity is destroyed.
     */
    protected open fun onSaveInstanceState(outState: Bundle) {
    }

    /**
     * Restores data that was saved in the [.onSaveInstanceState] method. This should be overridden
     * to restore this Controller's state to where it was before it was destroyed.
     */
    protected open fun onRestoreInstanceState(savedInstanceState: Bundle) {
    }

    /**
     * Calls startActivity(Intent) from this Controller's host Activity.
     */
    fun startActivity(intent: Intent) {
        withRouter { it.startActivity(intent) }
    }

    /**
     * Calls startActivityForResult(Intent, int) from this Controller's host Activity.
     */
    fun startActivityForResult(intent: Intent, requestCode: Int) {
        withRouter { it.startActivityForResult(instanceId, intent, requestCode) }
    }

    /**
     * Calls startActivityForResult(Intent, int, Bundle) from this Controller's host Activity.
     */
    fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        withRouter { it.startActivityForResult(instanceId, intent, requestCode, options) }
    }

    /**
     * Calls startIntentSenderForResult(IntentSender, int, Intent, int, int, int, Bundle) from this Controller's host Activity.
     */
    fun startIntentSenderForResult(
        intent: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int,
        flagsValues: Int, extraFlags: Int, options: Bundle?
    ) {
        withRouter {
            it.startIntentSenderForResult(
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
    }

    /**
     * Registers this Controller to handle onActivityResult responses. Calling this method is NOT
     * necessary when calling [.startActivityForResult]
     */
    fun registerForActivityResult(requestCode: Int) {
        withRouter { it.registerForActivityResult(instanceId, requestCode) }
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
        withRouter { it.requestPermissions(instanceId, permissions, requestCode) }
    }

    /**
     * Gets whether you should show UI with rationale for requesting a permission.
     * {@see android.app.Activity#shouldShowRequestPermissionRationale(String)}
     */
    open fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return Build.VERSION.SDK_INT >= 23
                && activity?.shouldShowRequestPermissionRationale(permission) ?: false
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
     * Should be overridden if this Controller needs to handle the back button being pressed.
     */
    open fun handleBack() = _childRouters
        .flatMap { it.backstack }
        .sortedByDescending { it.transactionIndex }
        .any { it.controller.isAttached && it.controller.requireRouter().handleBack() }

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
            getChildRouter(container, tag, true)!!

    /**
     * Returns the child router for [container] and [tag] if already created
     */
    fun getChildRouterOrNull(container: ViewGroup, tag: String? = null) =
        getChildRouter(container, tag, false)

    private fun getChildRouter(
        container: ViewGroup,
        tag: String?,
        createIfNeeded: Boolean
    ): Router? {
        val containerId = container.id

        var childRouter = _childRouters
            .firstOrNull { it.hostId == containerId && it.tag == tag }

        if (childRouter == null) {
            if (createIfNeeded) {
                childRouter = ControllerHostedRouter(
                    container.id,
                    tag
                )
                childRouter.setHost(this, container)
                _childRouters.add(childRouter)

                if (isPerformingExitTransition) {
                    childRouter.isDetachFrozen = true
                }
            }
        } else if (!childRouter.hasHost) {
            childRouter.setHost(this, container)
            childRouter.rebindIfNeeded()
        }

        return childRouter
    }

    /**
     * Removes a child [Router] from this Controller. When removed, all Controllers currently managed by
     * the [Router] will be destroyed.
     */
    fun removeChildRouter(childRouter: Router) {
        if (childRouter is ControllerHostedRouter && _childRouters.remove(childRouter)) {
            childRouter.destroy(true)
        }
    }

    /**
     * Adds option items to the host Activity's standard options menu. This will only be called if
     * [.setHasOptionsMenu] has been called.
     */
    open fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    }

    /**
     * Prepare the screen's options menu to be displayed. This is called directly before showing the
     * menu and can be used modify its contents.
     */
    open fun onPrepareOptionsMenu(menu: Menu) {
    }

    /**
     * Called when an option menu item has been selected by the user.
     */
    open fun onOptionsItemSelected(item: MenuItem) = false

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

    internal fun contextAvailable() {
        val context = requireRouter().activity

        if (context != null && !isContextAvailable) {
            notifyLifecycleListeners { it.preContextAvailable(this) }
            isContextAvailable = true
            onContextAvailable(context)
            notifyLifecycleListeners { it.postContextAvailable(this, context) }
        }

        _childRouters.forEach { it.onContextAvailable() }
    }

    internal fun activityStarted(activity: Activity) {
        viewAttachHandler?.onActivityStarted()
        onActivityStarted(activity)
    }

    internal fun activityResumed(activity: Activity) {
        val view = view
        if (!isAttached && view != null && viewIsAttached) {
            attach(view)
        } else if (isAttached) {
            needsAttach = false
            hasSavedViewState = false
        }

        onActivityResumed(activity)
    }

    internal fun activityPaused(activity: Activity) {
        onActivityPaused(activity)
    }

    internal fun activityStopped(activity: Activity) {
        viewAttachHandler?.onActivityStopped()

        if (isAttached && activity.isChangingConfigurations) {
            needsAttach = true
        }

        onActivityStopped(activity)
    }

    internal fun activityDestroyed(activity: Activity) {
        if (activity.isChangingConfigurations) {
            view?.let { detach(it, true, false, true) }
        } else {
            destroy(true)
        }

        if (isContextAvailable) {
            notifyLifecycleListeners { it.preContextUnavailable(this, activity) }
            isContextAvailable = false
            onContextUnavailable()
            notifyLifecycleListeners { it.postContextUnavailable(this) }
        }
    }

    internal fun inflate(parent: ViewGroup): View {
        var view = view
        if (view != null && view.parent != null && view.parent != parent) {
            detach(view, true, false, false)
            removeViewReference(false)
        }

        if (view == null) {
            notifyLifecycleListeners { it.preCreateView(this) }

            view = onCreateView(
                LayoutInflater.from(parent.context),
                parent,
                viewState
            ).also { this.view = it }

            if (view == parent) {
                throw IllegalStateException("Controller's onCreateView method returned the parent ViewGroup. Perhaps you forgot to pass false for LayoutInflater.inflate's attachToRoot parameter?")
            }

            notifyLifecycleListeners { it.postCreateView(this, view) }

            restoreViewState(view)

            viewAttachHandler = ViewAttachHandler(object : ViewAttachListener {
                override fun onAttached() {
                    viewIsAttached = true
                    viewWasDetached = false
                    attach(view)
                }

                override fun onDetached(fromActivityStop: Boolean) {
                    viewIsAttached = false
                    viewWasDetached = true

                    if (!isDetachFrozen) {
                        detach(view, false, fromActivityStop, false)
                    }
                }

                override fun onViewDetachAfterStop() {
                    if (!isDetachFrozen) {
                        detach(view, false, false, false)
                    }
                }
            }).also { it.listenForAttach(view) }
        } else if (retainViewMode == RetainViewMode.RETAIN_DETACH) {
            restoreChildControllerHosts()
        }

        return view
    }

    internal fun attach(view: View) {
        val router = router

        attachedToUnownedParent = router == null || view.parent != router.container

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
        needsAttach = router?.isActivityStopped == true

        onAttach(view)

        if (hasOptionsMenu && !optionsMenuHidden) {
            requireRouter().invalidateOptionsMenu()
        }

        notifyLifecycleListeners { it.postAttach(this, view) }

        _childRouters
            .flatMap { it.backstack }
            .filter { it.controller.awaitingParentAttach }
            .forEach { it.controller.attach(it.controller.view!!) }
    }

    internal fun detach(view: View, forceViewRemoval: Boolean, blockViewRemoval: Boolean, forceChildViewRemoval: Boolean) {
        if (!attachedToUnownedParent) {
            _childRouters.forEach { it.prepareForHostDetach() }
        }

        val removeViewRef =
            !blockViewRemoval && (forceViewRemoval || retainViewMode == RetainViewMode.RELEASE_DETACH || isBeingDestroyed)

        if (isAttached) {
            notifyLifecycleListeners { it.preDetach(this, view) }
            isAttached = false

            if (!awaitingParentAttach) {
                onDetach(view)
            }

            if (hasOptionsMenu && !optionsMenuHidden) {
                router?.invalidateOptionsMenu()
            }

            notifyLifecycleListeners { it.postDetach(this, view) }
        }

        if (removeViewRef) {
            removeViewReference(forceChildViewRemoval)
        }
    }

    private fun removeViewReference(forceChildViewRemoval: Boolean) {
        val view = view
        if (view != null) {
            if (!isBeingDestroyed && !hasSavedViewState) {
                saveViewState(view)
            }

            notifyLifecycleListeners { it.preDestroyView(this, view) }

            onDestroyView(view)

            viewAttachHandler?.unregisterAttachListener(view)
            viewAttachHandler = null
            viewIsAttached = false

            if (isBeingDestroyed) {
                destroyedView = WeakReference(view)
            }

            this.view = null

            notifyLifecycleListeners { it.postDestroyView(this) }

            _childRouters.forEach { it.removeHost(forceChildViewRemoval) }
        }

        if (isBeingDestroyed) {
            performDestroy()
        }
    }

    private fun restoreChildControllerHosts() {
        _childRouters
            .filterNot { it.hasHost }
            .forEach {
                val containerView = view?.findViewById(it.hostId) as? ViewGroup

                if (containerView != null) {
                    it.setHost(this, containerView)
                    it.rebindIfNeeded()
                }
            }
    }

    private fun performDestroy() {
        if (isContextAvailable) {
            notifyLifecycleListeners { it.preContextUnavailable(this, activity!!) }
            isContextAvailable = false
            onContextUnavailable()
            notifyLifecycleListeners { it.postContextUnavailable(this) }
        }

        if (!isDestroyed) {
            notifyLifecycleListeners { it.preDestroy(this) }
            isDestroyed = true

            onDestroy()

            parentController = null
            notifyLifecycleListeners { it.postDestroy(this) }
        }
    }

    internal fun destroy() {
        destroy(false)
    }

    private fun destroy(removeViews: Boolean) {
        isBeingDestroyed = true

        router?.unregisterForActivityResults(instanceId)

        _childRouters.forEach { it.destroy(false) }

        if (!isAttached) {
            removeViewReference(true)
        } else if (removeViews) {
            view?.let { detach(it, true, false, true) }
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
            .map { childRouter -> Bundle().also { childRouter.saveInstanceState(it) } }
        outState.putParcelableArrayList(KEY_CHILD_ROUTERS, ArrayList(childBundles))

        val savedState = Bundle(javaClass.classLoader)
        onSaveInstanceState(savedState)

        notifyLifecycleListeners { it.onSaveInstanceState(this, savedState) }

        outState.putBundle(KEY_SAVED_STATE, savedState)

        return outState
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
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

        savedInstanceState.getParcelableArrayList<Bundle>(KEY_CHILD_ROUTERS)
            ?.map { bundle -> ControllerHostedRouter().also { it.restoreInstanceState(bundle) } }
            ?.forEach { _childRouters.add(it) }

        this.savedInstanceState = savedInstanceState.getBundle(KEY_SAVED_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        performCreate()
    }

    private fun saveViewState(view: View) {
        hasSavedViewState = true

        val viewState = Bundle(javaClass.classLoader).also { this.viewState = it }

        val hierarchyState = SparseArray<Parcelable>()
        view.saveHierarchyState(hierarchyState)
        viewState.putSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY, hierarchyState)

        val stateBundle = Bundle(javaClass.classLoader)
        onSaveViewState(view, stateBundle)
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

            restoreChildControllerHosts()

            notifyLifecycleListeners { it.onRestoreViewState(this, viewState) }
        }
    }

    private fun performCreate() {
        if (!isCreated && router != null) {
            notifyLifecycleListeners { it.preCreate(this) }
            onCreate()
            isCreated = true
            notifyLifecycleListeners { it.postCreate(this) }
            performRestoreInstanceState()
        }
    }

    private fun performRestoreInstanceState() {
        val savedInstanceState = savedInstanceState
        if (savedInstanceState != null && router != null) {
            onRestoreInstanceState(savedInstanceState)

            notifyLifecycleListeners { it.onRestoreInstanceState(this, savedInstanceState) }

            this.savedInstanceState = null
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

        val destroyedView = destroyedView

        if (isBeingDestroyed && !viewIsAttached && !isAttached && destroyedView != null) {
            val view = destroyedView.get()
            val router = router
            val container = router?.container

            if (container != null && view != null && view.parent == router.container) {
                container.removeView(view)
            }

            this.destroyedView = null
        }
    }

    internal fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isAttached && hasOptionsMenu && !optionsMenuHidden) {
            onCreateOptionsMenu(menu, inflater)
        }
    }

    internal fun prepareOptionsMenu(menu: Menu) {
        if (isAttached && hasOptionsMenu && !optionsMenuHidden) {
            onPrepareOptionsMenu(menu)
        }
    }

    internal fun optionsItemSelected(item: MenuItem): Boolean {
        return isAttached && hasOptionsMenu && !optionsMenuHidden && onOptionsItemSelected(item)
    }

    private fun withRouter(action: (Router) -> Unit) {
        val router = router
        if (router != null) {
            action.invoke(router)
        } else {
            onRouterSetListeners.add(action)
        }
    }

    private inline fun notifyLifecycleListeners(block: (ControllerLifecycleListener) -> Unit) {
        lifecycleListeners.toList().forEach(block)
    }

    private inline fun onChildRouters(block: (ControllerHostedRouter) -> Unit) {
        _childRouters.forEach(block)
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
        private const val KEY_TARGET_INSTANCE_ID = "Controller.targedInstanceId"
        private const val KEY_ARGS = "Controller.args"
        private const val KEY_NEEDS_ATTACH = "Controller.needsAttach"
        private const val KEY_OVERRIDDEN_PUSH_HANDLER = "Controller.overriddenPushHandler"
        private const val KEY_OVERRIDDEN_POP_HANDLER = "Controller.overriddenPopHandler"
        private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
        private const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"
        private const val KEY_RETAIN_VIEW_MODE = "Controller.retainViewMode"

        internal fun fromBundle(bundle: Bundle): Controller {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            val clazz = classForNameOrThrow<Controller>(className)

            val args = bundle.getBundle(KEY_ARGS)
                ?.also { it.classLoader = clazz.classLoader }

            return newInstanceOrThrow<Controller>(className).apply {
                // Restore the args that existed before the last process death
                this.args = args ?: Bundle(clazz.classLoader!!)
                restoreInstanceState(bundle)
            }
        }
    }
}