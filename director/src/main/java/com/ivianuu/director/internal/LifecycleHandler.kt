package com.ivianuu.director.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.containerId
import kotlinx.android.parcel.Parcelize
import java.util.*

class LifecycleHandler : Fragment(), ActivityLifecycleCallbacks {

    private val routerMap =
        mutableMapOf<Int, ActivityHostedRouter>()

    private val activityRequests =
        mutableMapOf<Int, MutableSet<String>>()
    private val permissionRequests =
        mutableMapOf<Int, MutableSet<String>>()

    private var hasPreparedForHostDetach = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().application.registerActivityLifecycleCallbacks(this)

        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<RequestInstanceIdsPair>(KEY_ACTIVITY_REQUEST_CODES)
                ?.forEach { (requestCode, instanceIds) ->
                    activityRequests[requestCode] = instanceIds
                }

            savedInstanceState.getParcelableArrayList<RequestInstanceIdsPair>(
                KEY_PERMISSION_REQUEST_CODES)
                ?.forEach { (requestCode, instanceIds) ->
                    permissionRequests[requestCode] = instanceIds
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val activityRequests =
            activityRequests.entries.map { RequestInstanceIdsPair(it.key, it.value) }
        outState.putParcelableArrayList(KEY_ACTIVITY_REQUEST_CODES, ArrayList(activityRequests))

        val permissionRequests =
            permissionRequests.entries.map { RequestInstanceIdsPair(it.key, it.value) }
        outState.putParcelableArrayList(KEY_PERMISSION_REQUEST_CODES, ArrayList(permissionRequests))
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().application.unregisterActivityLifecycleCallbacks(this)

        routerMap.values.forEach { it.onActivityDestroyed() }
        routerMap.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        activityRequests[requestCode]?.let { instanceIds ->
            routerMap.values.forEach {
                it.onActivityResult(
                    instanceIds,
                    requestCode,
                    resultCode,
                    data
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionRequests[requestCode]?.let { instanceIds ->
            routerMap.values.forEach {
                it.onRequestPermissionsResult(
                    instanceIds,
                    requestCode,
                    permissions,
                    grantResults
                )
            }
        }
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return routerMap.values
            .filter { router ->
                router.shouldShowRequestPermissionRationale(
                    permission,
                    permissionRequests.flatMap { it.value }.toSet()
                )
            }
            .any() || super.shouldShowRequestPermissionRationale(permission)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        if (this.activity == activity) {
            hasPreparedForHostDetach = false
            routerMap.values.forEach { it.onActivityStarted() }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (this.activity == activity) {
            routerMap.values.forEach { it.onActivityResumed() }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (this.activity == activity) {
            routerMap.values.forEach { it.onActivityPaused() }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (this.activity == activity) {
            prepareForHostDetachIfNeeded()

            routerMap.values.forEach { router ->
                val bundle = Bundle().also { router.saveInstanceState(it) }
                outState.putBundle(KEY_ROUTER_STATE_PREFIX + router.containerId, bundle)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (this.activity == activity) {
            prepareForHostDetachIfNeeded()
            routerMap.values.forEach { it.onActivityStopped() }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    internal fun registerForActivityResult(instanceId: String, requestCode: Int) {
        activityRequests.getOrPut(requestCode) { mutableSetOf() }
            .add(instanceId)
    }

    internal fun unregisterForActivityResults(instanceId: String) {
        activityRequests
            .filterValues { it.contains(instanceId) }
            .keys
            .forEach { activityRequests.remove(it) }
    }

    internal fun startActivityForResult(instanceId: String, intent: Intent, requestCode: Int) {
        registerForActivityResult(instanceId, requestCode)
        startActivityForResult(intent, requestCode)
    }

    internal fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        registerForActivityResult(instanceId, requestCode)
        startActivityForResult(intent, requestCode, options)
    }

    @TargetApi(Build.VERSION_CODES.N)
    internal fun startIntentSenderForResult(
        instanceId: String, intent: IntentSender, requestCode: Int,
        fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int,
        options: Bundle?
    ) {
        registerForActivityResult(instanceId, requestCode)
        startIntentSenderForResult(
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
            options
        )
    }

    @TargetApi(Build.VERSION_CODES.M)
    internal fun requestPermissions(
        instanceId: String,
        permissions: Array<String>,
        requestCode: Int
    ) {
        permissionRequests.getOrPut(requestCode) { mutableSetOf() }
            .add(instanceId)
        requestPermissions(permissions, requestCode)
    }

    internal fun getRouter(
        container: ViewGroup,
        savedInstanceState: Bundle?,
        controllerFactory: ControllerFactory?
    ) =
        routerMap.getOrPut(container.id) {
            ActivityHostedRouter(this, container).apply {
                controllerFactory?.let { this.controllerFactory = it }
                savedInstanceState?.getBundle(KEY_ROUTER_STATE_PREFIX + container.id)?.let {
                    restoreInstanceState(it)
                }
            }
        }

    private fun prepareForHostDetachIfNeeded() {
        if (!hasPreparedForHostDetach) {
            hasPreparedForHostDetach = true
            routerMap.values.forEach { it.prepareForHostDetach() }
        }
    }

    companion object {
        private val FRAGMENT_TAG = LifecycleHandler::class.java.name

        private const val KEY_ACTIVITY_REQUEST_CODES = "LifecycleHandler.activityRequests"
        private const val KEY_PERMISSION_REQUEST_CODES = "LifecycleHandler.permissionRequests"
        private const val KEY_ROUTER_STATE_PREFIX = "LifecycleHandler.routerState"

        internal fun install(activity: FragmentActivity): LifecycleHandler {
            return (findInActivity(activity) ?: LifecycleHandler().also {
                activity.supportFragmentManager.beginTransaction()
                    .add(it, FRAGMENT_TAG)
                    .commitNow()
            })
        }

        private fun findInActivity(activity: Activity): LifecycleHandler? {
            return (activity as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(FRAGMENT_TAG) as? LifecycleHandler
        }
    }

    @Parcelize
    private data class RequestInstanceIdsPair(
        val requestCode: Int,
        val instanceIds: MutableSet<String>
    ) : Parcelable

}
