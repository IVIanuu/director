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

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.Router
import com.ivianuu.director.RouterManager

class RouterHostFragment : Fragment(), OnBackPressedCallback {

    private lateinit var manager: RouterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = RouterManager(
            requireActivity(),
            null,
            savedInstanceState?.getBundle(KEY_ROUTER_STATES)
        )

        requireActivity().addOnBackPressedCallback(this)
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

    internal fun getRouter(
        tag: String?,
        container: ViewGroup,
        controllerFactory: ControllerFactory?
    ): Router = manager.getRouter(container, tag, controllerFactory)

    override fun handleOnBackPressed(): Boolean = manager.handleBack()

    companion object {
        private const val FRAGMENT_TAG = "com.ivianuu.director.fragmenthost.RouterHostFragment"

        private const val KEY_ROUTER_STATES = "RouterHostFragment.routerState"

        internal fun install(fm: FragmentManager): RouterHostFragment {
            return (fm.findFragmentByTag(FRAGMENT_TAG) as? RouterHostFragment)
                ?: RouterHostFragment().also {
                fm.beginTransaction()
                    .add(it, FRAGMENT_TAG)
                    .commitNow()
                }
        }

    }

}