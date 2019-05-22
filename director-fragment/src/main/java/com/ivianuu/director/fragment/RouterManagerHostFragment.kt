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

package com.ivianuu.director.fragment

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.hasHost
import com.ivianuu.director.*

class RouterManagerHostFragment : Fragment() {

    private val manager by lazy(LazyThreadSafetyMode.NONE) {
        RouterManager(requireActivity())
    }

    private val backPressedCallback = BackPressedCallback()
    private val registeredRouters = mutableSetOf<Router>()
    private val routerListener = ControllerChangeListener(
        onChangeStarted = { _, _, _, _, _, _ ->
            backPressedCallback.isEnabled =
                manager.routers.any {
                    it.hasRoot && (it.popsLastView || it.backstackSize > 1)
                }
        }
    )

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
        requireActivity().onBackPressedDispatcher.addCallback(backPressedCallback)
    }

    override fun onStart() {
        super.onStart()
        manager.onStart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_ROUTER_STATES, manager.saveInstanceState())
    }

    override fun onStop() {
        super.onStop()
        manager.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.remove()
        manager.onDestroy()
    }

    private fun registerIfNeeded(router: Router) {
        if (registeredRouters.add(router)) {
            router.addChangeListener(routerListener, true)
        }
    }

    private inner class BackPressedCallback : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            manager.handleBack()
        }
    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.fragmenthost.RouterManagerHostFragment"

        private const val KEY_ROUTER_STATES = "RouterManagerHostFragment.routerState"

        private val postponedActivities = mutableListOf<FragmentActivity>()

        fun getRouterOrNull(
            activity: FragmentActivity,
            containerId: Int,
            tag: String? = null
        ): Router? {
            val host = get(activity)
            return host.manager.getRouterOrNull(containerId, tag)
                ?.also { host.registerIfNeeded(it) }
        }

        fun getRouterOrNull(
            activity: FragmentActivity,
            container: ViewGroup,
            tag: String? = null
        ): Router? {
            val host = get(activity)
            return host.manager.getRouterOrNull(container, tag)
                ?.also { host.registerIfNeeded(it) }
        }

        fun getRouter(activity: FragmentActivity, containerId: Int, tag: String? = null): Router {
            val host = get(activity)
            return host.manager.getRouter(containerId, tag)
                .also { host.registerIfNeeded(it) }
        }

        fun getRouter(
            activity: FragmentActivity,
            container: ViewGroup,
            tag: String? = null
        ): Router {
            val host = get(activity)
            return host.manager.getRouter(container, tag)
                .also { host.registerIfNeeded(it) }
        }

        fun removeRouter(activity: FragmentActivity, router: Router) {
            val host = get(activity)
            host.manager.removeRouter(router)
            host.registeredRouters.remove(router)
        }

        fun postponeFullRestore(activity: FragmentActivity) {
            if (activity.supportFragmentManager.hasHost) {
                get(activity).manager.postponeRestore()
            } else {
                postponedActivities.add(activity)
            }
        }

        fun startPostponedFullRestore(activity: FragmentActivity) {
            if (activity.supportFragmentManager.hasHost) {
                get(activity).manager.startPostponedFullRestore()
            } else {
                postponedActivities.remove(activity)
            }
        }

        fun getManager(activity: FragmentActivity): RouterManager =
            get(activity).manager

        private fun get(activity: FragmentActivity): RouterManagerHostFragment {
            return (activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? RouterManagerHostFragment)
                ?: RouterManagerHostFragment().also {
                    activity.supportFragmentManager.beginTransaction()
                    .add(it, FRAGMENT_TAG)
                    .commitNow()
                }
        }

    }

}