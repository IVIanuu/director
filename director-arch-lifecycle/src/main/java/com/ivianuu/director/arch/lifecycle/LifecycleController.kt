package com.ivianuu.director.arch.lifecycle

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.ivianuu.director.Controller

/**
 * A [Controller] which is also a [LifecycleOwner] and a [ViewModelStoreOwner]
 */
abstract class LifecycleController : Controller(), LifecycleOwner, ViewModelStoreOwner {

    private val lifecycleOwner = ControllerLifecycleOwner()
    private val viewModelStoreOwner = ControllerViewModelStoreOwner()

    val viewLifecycleOwner: LifecycleOwner = ControllerViewLifecycleOwner()

    override fun getLifecycle() = lifecycleOwner.lifecycle

    override fun getViewModelStore() = viewModelStoreOwner.viewModelStore

}