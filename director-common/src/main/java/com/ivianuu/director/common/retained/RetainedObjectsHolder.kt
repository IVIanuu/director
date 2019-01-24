package com.ivianuu.director.common.retained

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener

/**
 * Holds retained objects of controller
 */
class RetainedObjectsHolder : Fragment(), ControllerLifecycleListener {

    private val retainedObjects =
        mutableMapOf<String, RetainedObjects>()

    init {
        retainInstance = true
    }

    override fun postDestroy(controller: Controller) {
        super.postDestroy(controller)
        if (!controller.activity.isChangingConfigurations) {
            retainedObjects.remove(controller.instanceId)
        }
    }

    internal fun getRetainedObjects(controller: Controller): RetainedObjects {
        controller.router.addLifecycleListener(this)
        return retainedObjects.getOrPut(controller.instanceId) { RetainedObjects() }
    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.common.retained.RetainedObjectsHolder"

        internal fun get(controller: Controller): RetainedObjects {
            val activity = (controller.activity as? FragmentActivity)
                ?: error("controller is not attached to a FragmentActivity")
            return (findInActivity(controller.activity) ?: RetainedObjectsHolder().also {
                activity.supportFragmentManager.beginTransaction()
                    .add(it, FRAGMENT_TAG)
                    .commitNow()
            }).getRetainedObjects(controller)
        }

        private fun findInActivity(activity: Activity): RetainedObjectsHolder? {
            return (activity as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(FRAGMENT_TAG) as? RetainedObjectsHolder
        }
    }
}