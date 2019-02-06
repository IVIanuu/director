package com.ivianuu.director.sample.controller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import com.ivianuu.director.Controller
import com.ivianuu.director.activity
import com.ivianuu.director.activityresult.ActivityResultListener
import com.ivianuu.director.parentController
import com.ivianuu.director.permission.PermissionCallback
import com.ivianuu.director.sample.ActionBarProvider
import com.ivianuu.director.sample.util.LoggingControllerFactory
import com.ivianuu.director.sample.util.LoggingLifecycleListener
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController : Controller(), LayoutContainer, PermissionCallback,
    ActivityResultListener {

    override val containerView: View?
        get() = view

    protected open val layoutRes = 0
    var actionBarTitle: String? = null

    private val actionBar: ActionBar?
        get() = (activity as? ActionBarProvider)?.providedActionBar

    init {
        addLifecycleListener(LoggingLifecycleListener())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childRouters.forEach {
            it.controllerFactory =
                    LoggingControllerFactory()
        }

        super.onCreate(savedInstanceState)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
    }

    override fun toString(): String {
        return "${javaClass.simpleName} ${System.identityHashCode(this)}"
    }

}