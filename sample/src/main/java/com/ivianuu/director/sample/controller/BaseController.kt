package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import com.ivianuu.director.arch.lifecycle.LifecycleController

import com.ivianuu.director.sample.ActionBarProvider
import com.ivianuu.director.sample.util.LoggingControllerFactory
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController : LifecycleController(), LayoutContainer {

    override val containerView: View?
        get() = view

    protected open val layoutRes = 0
    var actionBarTitle: String? = null

    private val actionBar: ActionBar?
        get() = (activity as ActionBarProvider).providedActionBar

    override fun onCreate() {
        childRouters.forEach {
            it.controllerFactory =
                    LoggingControllerFactory()
        }
        super.onCreate()
    }

    override fun onInflateView(
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

    override fun onUnbindView(view: View) {
        clearFindViewByIdCache()
        super.onUnbindView(view)
    }

    protected fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.actionBarTitle != null) {
                return
            }
            parentController = parentController.parentController
        }

        val title = actionBarTitle
        val actionBar = actionBar
        if (title != null && actionBar != null) {
            actionBar.title = title
        }
    }

    override fun toString(): String {
        return "${javaClass.simpleName} ${System.identityHashCode(this)}"
    }
}
