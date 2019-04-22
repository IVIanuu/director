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
import androidx.activity.OnBackPressedCallback
import androidx.arch.core.util.Cancellable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.hasHost
import com.ivianuu.director.RouterManager

class RouterManagerHostFragment : Fragment(), OnBackPressedCallback {

    private val manager by lazy(LazyThreadSafetyMode.NONE) {
        RouterManager(requireActivity())
    }

    private var backPressCancellable: Cancellable? = null

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
        backPressCancellable =
            requireActivity().onBackPressedDispatcher.addCallback(this)
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
        backPressCancellable?.cancel()
        backPressCancellable = null
        manager.onDestroy()
    }

    override fun handleOnBackPressed(): Boolean = manager.handleBack()

    companion object {
        private const val FRAGMENT_TAG =
            "com.ivianuu.director.fragmenthost.RouterManagerHostFragment"

        private const val KEY_ROUTER_STATES = "RouterManagerHostFragment.routerState"

        private val postponedActivities = mutableListOf<FragmentActivity>()

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