package com.ivianuu.director

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

/**
 * Lightweight view controller with a lifecycle
 */
abstract class Controller {

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
     * The parent controller if any
     */
    val parentController: Controller? get() = router.parent

    /**
     * The view of this controller or null
     */
    var view: View? = null
        private set

    /**
     * The current state of this controller
     */
    var state: ControllerState = INITIALIZED
        private set

    private var viewState: SparseArray<Parcelable>? = null

    private val listeners = mutableListOf<ControllerLifecycleListener>()

    /**
     * Will be called once when this controller gets attached to its router
     */
    protected open fun onCreate() {
    }

    /**
     * Called when this Controller has been destroyed.
     */
    protected open fun onDestroy() {
    }

    /**
     * Creates the view for this controller
     */
    protected abstract fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup
    ): View

    /**
     * Called when the view of this controller gets destroyed
     */
    protected open fun onDestroyView(view: View) {
    }

    /**
     * Called when this Controller is attached to its container
     */
    protected open fun onAttach(view: View) {
    }

    /**
     * Called when this Controller is detached from its container
     */
    protected open fun onDetach(view: View) {
    }

    /**
     * Should be overridden if this Controller needs to handle the back button being pressed.
     */
    open fun handleBack(): Boolean = false

    /**
     * Adds a listener for all of this Controller's lifecycle events
     */
    fun addLifecycleListener(listener: ControllerLifecycleListener) {
        listeners.add(listener)
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeLifecycleListener(listener: ControllerLifecycleListener) {
        listeners.remove(listener)
    }

    internal fun create(router: Router) {
        _router = router

        // create
        notifyListeners { it.preCreate(this) }

        state = CREATED
        onCreate()

        notifyListeners { it.postCreate(this) }
    }

    internal fun destroy() {
        notifyListeners { it.preDestroy(this) }

        state = DESTROYED
        onDestroy()

        notifyListeners { it.postDestroy(this) }
    }

    internal fun createView(container: ViewGroup): View {
        notifyListeners { it.preCreateView(this) }

        val view = onCreateView(
            LayoutInflater.from(container.context),
            container
        ).also { this.view = it }

        state = VIEW_CREATED
        notifyListeners { it.postCreateView(this, view) }

        // restore hierarchy
        viewState?.let { view.restoreHierarchyState(it) }
        viewState = null

        return view
    }

    internal fun destroyView() {
        val view = requireView()
        if (viewState == null) {
            viewState = SparseArray<Parcelable>()
                .also { view.saveHierarchyState(it) }
        }

        notifyListeners { it.preDestroyView(this, view) }

        onDestroyView(view)
        this.view = null
        state = CREATED

        notifyListeners { it.postDestroyView(this) }
    }

    internal fun attach() {
        val view = requireView()

        notifyListeners { it.preAttach(this, view) }

        state = ATTACHED
        onAttach(view)
        viewState = null

        notifyListeners { it.postAttach(this, view) }
    }

    internal fun detach() {
        val view = requireView()

        notifyListeners { it.preDetach(this, view) }

        state = VIEW_CREATED
        onDetach(view)

        notifyListeners { it.postDetach(this, view) }
    }

    private inline fun notifyListeners(block: (ControllerLifecycleListener) -> Unit) {
        (listeners + router.getControllerLifecycleListeners()).forEach(block)
    }

}

val Controller.isCreated: Boolean get() = state.isAtLeast(CREATED)

val Controller.isViewCreated: Boolean get() = state.isAtLeast(VIEW_CREATED)

val Controller.isAttached: Boolean get() = state.isAtLeast(ATTACHED)

val Controller.isDestroyed: Boolean get() = state == DESTROYED

fun Controller.requireView(): View =
    view ?: error("view is only accessible between onCreateView and onDestroyView")

fun Controller.toTransaction(): RouterTransaction = RouterTransaction(this)

fun Controller.childRouter(container: ViewGroup): Router =
    childRouter { container }

fun Controller.childRouter(containerId: Int): Router =
    childRouter { view!!.findViewById(containerId) }

fun Controller.childRouter(containerProvider: (() -> ViewGroup)? = null): Router {
    val router = Router(this)

    if (isViewCreated) {
        containerProvider?.invoke()?.let { router.setContainer(it) }
    }

    addLifecycleListener(
        postCreateView = { _, _ ->
            containerProvider?.invoke()?.let { router.setContainer(it) }
        },
        postAttach = { _, _ ->
            router.start()
        },
        preDetach = { _, _ ->
            router.stop()
        },
        preDestroyView = { _, _ ->
            if (containerProvider != null) {
                router.removeContainer()
            }
        },
        preDestroy = {
            router.destroy()
        }
    )

    return router
}