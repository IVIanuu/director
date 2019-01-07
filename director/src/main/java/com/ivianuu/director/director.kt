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
@JvmName("attachRouterWithReceiver")
fun FragmentActivity.attachRouter(
    container: ViewGroup,
    savedInstanceState: Bundle?,
    controllerFactory: ControllerFactory? = null
): Router = attachRouter(this, container, savedInstanceState, controllerFactory)

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
fun attachRouter(
    activity: FragmentActivity,
    container: ViewGroup,
    savedInstanceState: Bundle? = null,
    controllerFactory: ControllerFactory? = null
): Router {
    val lifecycleHandler = LifecycleHandler.install(activity)

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
@JvmName("attachRouterWithReceiver")
fun FragmentActivity.attachRouter(
    containerId: Int,
    savedInstanceState: Bundle?,
    controllerFactory: ControllerFactory? = null
): Router = attachRouter(findViewById<ViewGroup>(containerId), savedInstanceState, controllerFactory)

/**
 * Director will create a [Router] that has been initialized for your Activity and containing ViewGroup.
 * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
 * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
 * an empty backstack.
 */
fun attachRouter(
    activity: FragmentActivity,
    containerId: Int,
    savedInstanceState: Bundle? = null,
    controllerFactory: ControllerFactory? = null
): Router = attachRouter(
    activity,
    activity.findViewById<ViewGroup>(containerId),
    savedInstanceState,
    controllerFactory
)