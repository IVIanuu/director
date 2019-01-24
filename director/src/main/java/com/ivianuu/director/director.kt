package com.ivianuu.director

import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.internal.LifecycleHandler

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
fun FragmentActivity.attachRouter(
    container: ViewGroup,
    savedInstanceState: Bundle? = null,
    controllerFactory: ControllerFactory? = null
): Router {
    val lifecycleHandler = LifecycleHandler.install(this)

    val router = lifecycleHandler.getRouter(container, savedInstanceState, controllerFactory)
    router.rebind()

    return router
}

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
fun FragmentActivity.attachRouter(
    containerId: Int,
    savedInstanceState: Bundle?,
    controllerFactory: ControllerFactory? = null
): Router = attachRouter(
    findViewById<ViewGroup>(containerId),
    savedInstanceState,
    controllerFactory
)