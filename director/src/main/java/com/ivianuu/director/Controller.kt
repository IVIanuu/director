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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Lightweight view controller with a lifecycle
 */
abstract class Controller : LifecycleOwner, ViewModelStoreOwner {

    private lateinit var _router: Router
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

    private val _viewModelStore = ViewModelStore()

    private val lifecycleRegistry = LifecycleRegistry(this)

    private var _viewLifecycleOwner: ControllerViewLifecycleOwner? = null
    val viewLifecycleOwner: LifecycleOwner
        get() =
            _viewLifecycleOwner
                ?: error("can only access the viewLifecycleOwner while the view is created")

    private var _viewLifecycleOwnerLiveData = MutableLiveData<LifecycleOwner?>()
    val viewLifecycleOwnerLiveData: LiveData<LifecycleOwner?>
        get() = _viewLifecycleOwnerLiveData


    /**
     * Will be called once when this controller gets attached to its router
     */
    protected open fun onCreate() {
    }

    /**
     * Creates the view for this controller
     */
    protected abstract fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup
    ): View

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
     * Called when the view of this controller gets destroyed
     */
    protected open fun onDestroyView(view: View) {
    }

    /**
     * Called when this Controller has been destroyed.
     */
    protected open fun onDestroy() {
    }

    /**
     * Should be overridden if this Controller needs to handle the back button being pressed.
     */
    open fun handleBack(): Boolean = false

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun getViewModelStore(): ViewModelStore = _viewModelStore

    internal fun create(router: Router) {
        _router = router
        onCreate()
        lifecycleRegistry.currentState = CREATED
    }

    internal fun destroy() {
        lifecycleRegistry.currentState = DESTROYED
        onDestroy()
        _viewModelStore.clear()
    }

    internal fun createView(container: ViewGroup): View {
        _viewLifecycleOwner = ControllerViewLifecycleOwner()

        val view = onCreateView(
            LayoutInflater.from(container.context),
            container
        ).also { this.view = it }

        _viewLifecycleOwner!!.currentState = CREATED
        _viewLifecycleOwnerLiveData.value = _viewLifecycleOwner

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

        _viewLifecycleOwner!!.currentState = DESTROYED
        onDestroyView(view)
        this.view = null
        _viewLifecycleOwner = null
        _viewLifecycleOwnerLiveData.value = null
    }

    internal fun attach() {
        val view = requireView()

        onAttach(view)
        viewState = null

        lifecycleRegistry.currentState = STARTED
        _viewLifecycleOwner!!.currentState = STARTED
        lifecycleRegistry.currentState = RESUMED
        _viewLifecycleOwner!!.currentState = RESUMED
    }

    internal fun detach() {
        val view = requireView()

        lifecycleRegistry.currentState = STARTED
        _viewLifecycleOwner!!.currentState = STARTED
        lifecycleRegistry.currentState = CREATED
        _viewLifecycleOwner!!.currentState = CREATED

        onDetach(view)
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

fun Controller.requireParentController(): Controller =
    parentController ?: error("parent controller == null")

fun Controller.toTransaction(): RouterTransaction = RouterTransaction(this)