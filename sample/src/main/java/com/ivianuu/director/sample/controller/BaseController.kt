package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import com.ivianuu.director.arch.lifecycle.LifecycleController
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.ActionBarProvider
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController : LifecycleController(), LayoutContainer {

    override var containerView: View? = null

    protected open val layoutRes = 0
    protected var title: String? = null

    private val actionBar: ActionBar?
        get() = (requireActivity() as ActionBarProvider).providedActionBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return if (layoutRes != 0) {
            inflater.inflate(layoutRes, container, false)
                .also { containerView = it }
                .also { onViewCreated(it) }
        } else {
            throw IllegalStateException("no layout res provided")
        }
    }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
    }

    protected open fun onViewCreated(view: View) {
    }

    override fun onDestroyView(view: View) {
        containerView = null
        clearFindViewByIdCache()
        super.onDestroyView(view)
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
