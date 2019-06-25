package com.ivianuu.director

import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Lightweight view controller with a lifecycle
 */
abstract class Controller : LifecycleOwner, ViewModelStoreOwner {

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

    private var viewState: SparseArray<Parcelable>? = null

    private val listeners = mutableListOf<ControllerLifecycleListener>()

    private val _viewModelStore = ViewModelStore()

    private val lifecycleRegistry = LifecycleRegistry(this)

    val viewLifecycleOwner: LifecycleOwner
        get() =
            _viewLifecycleOwner
                ?: error("can only access the viewLifecycleOwner while the view is created")
    private var _viewLifecycleOwner: ControllerViewLifecycleOwner? = null

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

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun getViewModelStore(): ViewModelStore = _viewModelStore

    internal fun create(router: Router) {
        _router = router

        // create
        notifyListeners { it.preCreate(this) }

        onCreate()
        lifecycleRegistry.currentState = CREATED

        notifyListeners { it.postCreate(this) }
    }

    internal fun destroy() {
        notifyListeners { it.preDestroy(this) }

        lifecycleRegistry.currentState = DESTROYED
        onDestroy()

        notifyListeners { it.postDestroy(this) }

        _viewModelStore.clear()
    }

    internal fun createView(container: ViewGroup): View {
        notifyListeners { it.preCreateView(this) }

        _viewLifecycleOwner = ControllerViewLifecycleOwner()

        val view = onCreateView(
            LayoutInflater.from(container.context),
            container
        ).also { this.view = it }

        _viewLifecycleOwner!!.currentState = CREATED

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

        _viewLifecycleOwner!!.currentState = DESTROYED
        onDestroyView(view)
        this.view = null
        _viewLifecycleOwner = null

        notifyListeners { it.postDestroyView(this) }
    }

    internal fun attach() {
        val view = requireView()

        notifyListeners { it.preAttach(this, view) }

        onAttach(view)
        viewState = null

        lifecycleRegistry.currentState = STARTED
        _viewLifecycleOwner!!.currentState = STARTED
        lifecycleRegistry.currentState = RESUMED
        _viewLifecycleOwner!!.currentState = RESUMED

        notifyListeners { it.postAttach(this, view) }
    }

    internal fun detach() {
        val view = requireView()

        notifyListeners { it.preDetach(this, view) }

        lifecycleRegistry.currentState = STARTED
        _viewLifecycleOwner!!.currentState = STARTED
        lifecycleRegistry.currentState = CREATED
        _viewLifecycleOwner!!.currentState = CREATED

        onDetach(view)

        notifyListeners { it.postDetach(this, view) }
    }

    private inline fun notifyListeners(block: (ControllerLifecycleListener) -> Unit) {
        (listeners + router.getControllerLifecycleListeners()).forEach(block)
    }

    private class ControllerViewLifecycleOwner : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        var currentState: Lifecycle.State
            get() = lifecycleRegistry.currentState
            set(value) {
                lifecycleRegistry.currentState = value
            }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }

}

fun Controller.requireView(): View =
    view ?: error("view is only accessible between onCreateView and onDestroyView")

fun Controller.toTransaction(): RouterTransaction = RouterTransaction(this)