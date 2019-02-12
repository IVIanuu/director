package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.activity
import com.ivianuu.director.parentController
import com.ivianuu.director.sample.ToolbarProvider
import com.ivianuu.director.sample.util.LoggingLifecycleListener
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController : Controller(), LayoutContainer {

    override val containerView: View?
        get() = view

    protected open val layoutRes = 0
    var toolbarTitle: String? = null

    init {
        addLifecycleListener(LoggingLifecycleListener())
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        check(layoutRes != 0) { "no layout res provided" }
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
    }

    override fun onUnbindView(view: View) {
        clearFindViewByIdCache()
        super.onUnbindView(view)
    }

    protected fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.toolbarTitle != null) {
                return
            }
            parentController = parentController.parentController
        }

        (activity as? ToolbarProvider)?.toolbar?.title = toolbarTitle
    }

    override fun toString(): String {
        return "${javaClass.simpleName} ${System.identityHashCode(this)}"
    }

}