package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.activity
import com.ivianuu.director.parentController
import com.ivianuu.director.sample.ToolbarProvider
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController : Controller(), LayoutContainer {

    override val containerView: View?
        get() = _containerView

    private var _containerView: View? = null

    protected open val layoutRes = 0

    protected open val toolbarTitle: String? get() = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        check(layoutRes != 0) { "no layout res provided" }
        val view = inflater.inflate(layoutRes, container, false)
            .also { _containerView = it }

        onViewCreated(view, savedViewState)

        return view
    }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
    }

    override fun onDestroyView(view: View) {
        clearFindViewByIdCache()
        _containerView = null
        super.onDestroyView(view)
    }

    private fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.toolbarTitle != null) {
                return
            }
            parentController = parentController.parentController
        }

        (activity as? ToolbarProvider)?.toolbar?.title = toolbarTitle
    }

    protected open fun onViewCreated(view: View, savedViewState: Bundle?) {
    }

}