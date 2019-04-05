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
import com.ivianuu.closeable.Closeable
import com.ivianuu.director.ControllerState.ATTACHED
import com.ivianuu.director.ControllerState.CREATED
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.ControllerState.INITIALIZED
import com.ivianuu.director.ControllerState.VIEW_CREATED
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
     * The transaction index of this controller
     */
    var transactionIndex: Int = -1
        internal set

    /**
     * The tag of this controller
     */
    var tag: String? = null
        set(value) {
            check(!this::_router.isInitialized) {
                "Cannot be changed after being added to a router"
            }
            field = value
        }

    /**
     * The current state of this controller
     */
    var state: ControllerState = INITIALIZED
        private set

    private var allState: Bundle? = null
    private var viewState: Bundle? = null

    /**
     * The change handler being used when this controller enters the screen
     */
    var pushChangeHandler: ChangeHandler? = DirectorPlugins.defaultPushHandler
        set(value) {
            check(!this::_router.isInitialized) {
                "Cannot be changed after being added to a router"
            }
            field = value
        }

    /**
     * The change handler being used when this controller exits the screen
     */
    var popChangeHandler: ChangeHandler? = DirectorPlugins.defaultPopHandler
        set(value) {
            check(!this::_router.isInitialized) {
                "Cannot be changed after being added to a router"
            }
            field = value
        }

    /**
     * The child router manager of this controller
     */
    val childRouterManager = RouterManager(this, postponeFullRestore = true)

    private val listeners = mutableListOf<ControllerListener>()

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
    fun addListener(listener: ControllerListener): Closeable {
        listeners.add(listener)
        return Closeable { removeListener(listener) }
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeListener(listener: ControllerListener) {
        listeners.remove(listener)
    }

    internal fun create(router: Router) {
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

    internal fun destroy() {
        if (state == DESTROYED) return

        childRouterManager.onDestroy()

        notifyListeners { it.preDestroy(this) }

        state = DESTROYED

        requireSuperCalled(this::onDestroy)

        notifyListeners { it.postDestroy(this) }
    }

    internal fun createView(container: ViewGroup): View {
        val savedViewState = viewState?.getBundle(KEY_VIEW_STATE_BUNDLE)

        notifyListeners { it.preCreateView(this, savedViewState) }

        val view = onCreateView(
            LayoutInflater.from(container.context),
            container,
            savedViewState
        ).also { this.view = it }

        state = VIEW_CREATED

        notifyListeners { it.postCreateView(this, view, savedViewState) }

        viewState?.getSparseParcelableArray<Parcelable>(KEY_VIEW_STATE_HIERARCHY)
            ?.let(view::restoreHierarchyState)

        if (savedViewState != null) {
            requireSuperCalled { onRestoreViewState(view, savedViewState) }
            notifyListeners { it.onRestoreViewState(this, view, savedViewState) }
        }

        viewState = null

        view.safeAs<ViewGroup>()?.let(childRouterManager::setRootView)

        return view
    }

    internal fun destroyView(saveViewState: Boolean) {
        val view = view ?: return
        if (saveViewState && viewState == null) {
            saveViewState()
        }

        childRouterManager.removeRootView()

        notifyListeners { it.preDestroyView(this, view) }

        requireSuperCalled { onDestroyView(view) }

        this.view = null

        notifyListeners { it.postDestroyView(this) }
    }

    internal fun attach() {
        if (isAttached) return

        val view = view ?: return

        notifyListeners { it.preAttach(this, view) }

        state = ATTACHED

        requireSuperCalled { onAttach(view) }

        notifyListeners { it.postAttach(this, view) }

        viewState = null

        childRouterManager.onStart()
    }

    internal fun detach() {
        if (!isAttached) return

        val view = view ?: return

        childRouterManager.onStop()

        notifyListeners { it.preDetach(this, view) }

        state = VIEW_CREATED

        requireSuperCalled { onDetach(view) }

        notifyListeners { it.postDetach(this, view) }
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
        outState.putString(KEY_TAG, tag)
        outState.putInt(KEY_TRANSACTION_INDEX, transactionIndex)

        pushChangeHandler?.let { outState.putBundle(KEY_PUSH_CHANGE_HANDLER, it.toBundle()) }
        popChangeHandler?.let { outState.putBundle(KEY_POP_CHANGE_HANDLER, it.toBundle()) }

        val savedState = Bundle(javaClass.classLoader)
        requireSuperCalled { onSaveInstanceState(savedState) }
        notifyListeners { it.onSaveInstanceState(this, savedState) }
        outState.putBundle(KEY_SAVED_STATE, savedState)

        outState.putBundle(KEY_CHILD_ROUTER_STATES, childRouterManager.saveInstanceState())

        return outState
    }

    private fun restoreInternalState() {
        val savedInstanceState = allState ?: return

        args = savedInstanceState.getBundle(KEY_ARGS)
            ?.also { it.classLoader = javaClass.classLoader }
            ?: args

        viewState = savedInstanceState.getBundle(KEY_VIEW_STATE)
            ?.also { it.classLoader = javaClass.classLoader }

        instanceId = savedInstanceState.getString(KEY_INSTANCE_ID)!!
        tag = savedInstanceState.getString(KEY_TAG)
        transactionIndex = savedInstanceState.getInt(KEY_TRANSACTION_INDEX)

        pushChangeHandler = savedInstanceState.getBundle(KEY_PUSH_CHANGE_HANDLER)
            ?.let(ChangeHandler.Companion::fromBundle)
        popChangeHandler = savedInstanceState.getBundle(KEY_POP_CHANGE_HANDLER)
            ?.let(ChangeHandler.Companion::fromBundle)

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
        private const val KEY_TAG = "Controller.tag"
        private const val KEY_TRANSACTION_INDEX = "Controller.transactionIndex"
        private const val KEY_PUSH_CHANGE_HANDLER = "Controller.pushChangeHandler"
        private const val KEY_POP_CHANGE_HANDLER = "Controller.popChangeHandler"
        private const val KEY_ARGS = "Controller.args"
        private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
        private const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"

        internal fun fromBundle(bundle: Bundle, factory: ControllerFactory): Controller {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            val cls = classForNameOrThrow(className)

            return factory.createController(cls.classLoader!!, className).apply {
                this.allState = bundle
                restoreInternalState()
            }
        }
    }
}

val Controller.isCreated: Boolean get() = state.isAtLeast(CREATED)

val Controller.isViewCreated: Boolean get() = state.isAtLeast(VIEW_CREATED)

val Controller.isAttached: Boolean get() = state.isAtLeast(ATTACHED)

val Controller.isDestroyed: Boolean get() = state == DESTROYED

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

fun Controller.tag(tag: String?): Controller = apply { this.tag = tag }

fun Controller.pushChangeHandler(changeHandler: ChangeHandler?): Controller =
    apply {
        pushChangeHandler = changeHandler
    }

fun Controller.popChangeHandler(changeHandler: ChangeHandler?): Controller =
    apply {
        popChangeHandler = changeHandler
    }

fun Controller.changeHandler(changeHandler: ChangeHandler?): Controller =
    pushChangeHandler(changeHandler).popChangeHandler(changeHandler)