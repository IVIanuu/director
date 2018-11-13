package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import com.ivianuu.director.arch.lifecycle.LifecycleController
import com.ivianuu.director.common.ControllerLayoutContainer
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.ActionBarProvider
import com.ivianuu.director.sample.LoggingControllerFactory
import kotlinx.android.extensions.LayoutContainer

abstract class BaseController : LifecycleController(), LayoutContainer {

    override val containerView: View?
        get() = controllerLayoutContainer.containerView
    private val controllerLayoutContainer = ControllerLayoutContainer()

    protected open val layoutRes = 0
    protected var title: String? = null

    private val actionBar: ActionBar?
        get() = (requireActivity() as ActionBarProvider).providedActionBar

    override fun onCreate() {
        childRouters.forEach { it.controllerFactory = LoggingControllerFactory() }
        super.onCreate()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return if (layoutRes != 0) {
            inflater.inflate(layoutRes, container, false)
        } else {
            throw IllegalStateException("no layout res provided")
        }
    }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
    }

    protected open fun onBindView(view: View) {

    }

    protected open fun onUnbindView(view: View) {
    }

    protected fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.title != null) {
                return
            }
            parentController = parentController.parentController
        }

        val title = title
        val actionBar = actionBar
        if (title != null && actionBar != null) {
            actionBar.title = title
        }
    }

    override fun toString(): String {
        return "${javaClass.simpleName} ${System.identityHashCode(this)}"
    }
}
