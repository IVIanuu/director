/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director.fragmenthost

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.host
import com.ivianuu.director.Router
import com.ivianuu.director.RouterManager
import com.ivianuu.director.getRouter
import com.ivianuu.director.getRouterOrNull

class RouterHostFragment : Fragment(), OnBackPressedCallback {

    private val manager by lazy(LazyThreadSafetyMode.NONE) {
        RouterManager(requireActivity(), null)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (postponedActivities.contains(requireActivity())) {
            manager.postponeRestore()
            postponedActivities.remove(requireActivity())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager.restoreInstanceState(savedInstanceState?.getBundle(KEY_ROUTER_STATES))
    }

    override fun onStart() {
        super.onStart()
        manager.hostStarted()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_ROUTER_STATES, manager.saveInstanceState())
    }

    override fun onStop() {
        super.onStop()
        manager.hostStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().removeOnBackPressedCallback(this)
        manager.hostDestroyed()
    }

    override fun handleOnBackPressed(): Boolean = manager.handleBack()

    companion object {
        private const val FRAGMENT_TAG = "com.ivianuu.director.fragmenthost.RouterHostFragment"

        private const val KEY_ROUTER_STATES = "RouterHostFragment.routerState"

        private val postponedActivities = mutableListOf<FragmentActivity>()

        internal fun getRouterOrNull(
            activity: FragmentActivity,
            containerId: Int,
            tag: String?
        ): Router? = get(activity).manager.getRouterOrNull(containerId, tag)

        fun getRouterOrNull(
            activity: FragmentActivity,
            container: ViewGroup,
            tag: String? = null
        ): Router? =
            get(activity).manager.getRouterOrNull(container, tag)

        fun getRouter(
            activity: FragmentActivity,
            containerId: Int,
            tag: String? = null
        ): Router =
            get(activity).manager.getRouter(containerId, tag)

        fun getRouter(
            activity: FragmentActivity,
            container: ViewGroup,
            tag: String? = null
        ): Router =
            get(activity).manager.getRouter(container, tag)

        fun removeRouter(
            activity: FragmentActivity,
            router: Router
        ) {
            get(activity).manager.removeRouter(router)
        }

        fun postponeFullRestore(activity: FragmentActivity) {
            if (activity.supportFragmentManager.host != null) {
                get(activity).manager.postponeRestore()
            } else {
                postponedActivities.add(activity)
            }
        }

        fun startPostponedFullRestore(activity: FragmentActivity) {
            if (activity.supportFragmentManager.host != null) {
                get(activity).manager.startPostponedFullRestore()
            } else {
                postponedActivities.remove(activity)
            }
        }

        private fun get(activity: FragmentActivity): RouterHostFragment {
            return (activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? RouterHostFragment)
                ?: RouterHostFragment().also {
                    activity.supportFragmentManager.beginTransaction()
                    .add(it, FRAGMENT_TAG)
                    .commitNow()
                }
        }

    }

}