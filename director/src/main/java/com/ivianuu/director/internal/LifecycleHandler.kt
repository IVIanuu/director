package com.ivianuu.director.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.Router
import kotlinx.android.parcel.Parcelize
import java.util.*

class LifecycleHandler : Fragment(), ActivityLifecycleCallbacks {

    internal val routers: List<Router>
        get() = routerMap.values.toList()

    private val routerMap = mutableMapOf<Int, ActivityHostedRouter>()

    private var activityRequestMap =
        mutableMapOf<Int, MutableSet<String>>()
    private var permissionRequestMap =
        mutableMapOf<Int, MutableSet<String>>()

    private var pendingPermissionRequests = ArrayList<PendingPermissionRequest>()

    private var destroyed = false
    private var attached = false
    private var hasPreparedForHostDetach = false

    private var hasRegisteredCallbacks = false

    init {
        retainInstance = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        attached = true

        pendingPermissionRequests
            .reversed()
            .forEach {
                pendingPermissionRequests.remove(it)
                requestPermissions(it.instanceId, it.permissions, it.requestCode)
            }

        destroyed = false

        routers.forEach { it.onContextAvailable() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<RequestInstanceIdsPair>(KEY_ACTIVITY_REQUEST_CODES)
                ?.forEach { (requestCode, instanceIds) ->
                    activityRequestMap[requestCode] = instanceIds
                }

            savedInstanceState.getParcelableArrayList<RequestInstanceIdsPair>(
                KEY_PERMISSION_REQUEST_CODES)
                ?.forEach { (requestCode, instanceIds) ->
                    permissionRequestMap[requestCode] = instanceIds
                }

            val pendingRequests =
                savedInstanceState.getParcelableArrayList<PendingPermissionRequest>(
                    KEY_PENDING_PERMISSION_REQUESTS
                )
            pendingPermissionRequests = pendingRequests ?: arrayListOf()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val activityRequests =
            activityRequestMap.entries.map { RequestInstanceIdsPair(it.key, it.value) }
        outState.putParcelableArrayList(KEY_ACTIVITY_REQUEST_CODES, ArrayList(activityRequests))

        val permissionRequests =
            permissionRequestMap.entries.map { RequestInstanceIdsPair(it.key, it.value) }
        outState.putParcelableArrayList(KEY_PERMISSION_REQUEST_CODES, ArrayList(permissionRequests))

        outState.putParcelableArrayList(KEY_PENDING_PERMISSION_REQUESTS, pendingPermissionRequests)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().application.unregisterActivityLifecycleCallbacks(this)
        destroyRouters()
    }

    override fun onDetach() {
        super.onDetach()
        attached = false
        destroyRouters()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        activityRequestMap[requestCode]?.let { instanceIds ->
            routers.forEach { it.onActivityResult(instanceIds, requestCode, resultCode, data) }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionRequestMap[requestCode]?.let { instanceIds ->
            routers.forEach { it.onRequestPermissionsResult(instanceIds, requestCode, permissions, grantResults) }
        }
    }

    override fun shouldShowRequestPermissionRationale(permission: String) = routers
        .filter { it.handleRequestedPermission(permission) }
        .any() || super.shouldShowRequestPermissionRationale(permission)

    internal fun registerForActivityResult(instanceId: String, requestCode: Int) {
        activityRequestMap.getOrPut(requestCode) { mutableSetOf() }
            .add(instanceId)
    }

    internal fun unregisterForActivityResults(instanceId: String) {
        activityRequestMap
            .filterValues { it.contains(instanceId) }
            .keys
            .forEach { activityRequestMap.remove(it) }
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
        if (attached) {
            permissionRequestMap.getOrPut(requestCode) { mutableSetOf() }
                .add(instanceId)
            requestPermissions(permissions, requestCode)
        } else {
            pendingPermissionRequests.add(
                PendingPermissionRequest(
                    instanceId,
                    permissions,
                    requestCode
                )
            )
        }
    }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (this.activity == activity) {
            routers.forEach { it.onContextAvailable() }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (this.activity == activity) {
            hasPreparedForHostDetach = false
            routers.forEach { it.onActivityStarted(activity) }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (this.activity == activity) {
            routers.forEach { it.onActivityResumed(activity) }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (this.activity == activity) {
            routers.forEach { it.onActivityPaused(activity) }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (this.activity == activity) {
            prepareForHostDetachIfNeeded()
            routers.forEach { it.onActivityStopped(activity) }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (this.activity == activity) {
            prepareForHostDetachIfNeeded()

            routers.forEach { router ->
                val bundle = Bundle().also { router.saveInstanceState(it) }
                outState.putBundle(KEY_ROUTER_STATE_PREFIX + router.containerId, bundle)
            }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    internal fun router(
        container: ViewGroup,
        savedInstanceState: Bundle?,
        controllerFactory: ControllerFactory?
    ) =
        routerMap.getOrPut(container.id) {
            ActivityHostedRouter().apply {
                setHost(this@LifecycleHandler, container)
                controllerFactory?.let { this.controllerFactory = it }
                savedInstanceState?.getBundle(KEY_ROUTER_STATE_PREFIX + container.id)?.let {
                    restoreInstanceState(it)
                }
            }
        }.also { it.setHost(this, container) }

    private fun destroyRouters() {
        if (!destroyed) {
            activity?.let { act -> routers.forEach { it.onActivityDestroyed(act) } }
            destroyed = true
        }
    }

    private fun prepareForHostDetachIfNeeded() {
        if (!hasPreparedForHostDetach) {
            hasPreparedForHostDetach = true
            routers.forEach { it.prepareForHostDetach() }
        }
    }

    private fun registerCallbacksIfNeeded(activity: FragmentActivity) {
        if (!hasRegisteredCallbacks) {
            activity.application.registerActivityLifecycleCallbacks(this)
            hasRegisteredCallbacks = true
        }
    }

    companion object {
        private const val FRAGMENT_TAG = "LifecycleHandler"

        private const val KEY_ACTIVITY_REQUEST_CODES = "LifecycleHandler.activityRequests"
        private const val KEY_PENDING_PERMISSION_REQUESTS = "LifecycleHandler.pendingPermissionRequests"
        private const val KEY_PERMISSION_REQUEST_CODES = "LifecycleHandler.permissionRequests"
        private const val KEY_ROUTER_STATE_PREFIX = "LifecycleHandler.routerState"

        internal fun install(activity: FragmentActivity) =
            (findInActivity(activity) ?: LifecycleHandler().also {
                activity.supportFragmentManager.beginTransaction()
                    .add(it, FRAGMENT_TAG)
                    .commitNow()
            }).also { it.registerCallbacksIfNeeded(activity) }

        private fun findInActivity(activity: Activity) =
            (activity as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(FRAGMENT_TAG) as? LifecycleHandler
    }

    @Parcelize
    private data class PendingPermissionRequest(
        val instanceId: String,
        val permissions: Array<String>,
        val requestCode: Int
    ) : Parcelable

    @Parcelize
    private data class RequestInstanceIdsPair(
        val requestCode: Int,
        val instanceIds: MutableSet<String>
    ) : Parcelable

}
