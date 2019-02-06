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
import com.ivianuu.director.RouterDelegate

class RouterHostFragment : Fragment(), OnBackPressedCallback {

    private lateinit var delegate: RouterDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = RouterDelegate(
            requireActivity(),
            null,
            savedInstanceState?.getBundle(KEY_ROUTER_STATES)
        )

        requireActivity().addOnBackPressedCallback(this)
    }

    override fun onStart() {
        super.onStart()
        delegate.hostStarted()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_ROUTER_STATES, delegate.saveInstanceState())
    }

    override fun onStop() {
        super.onStop()
        delegate.hostStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().removeOnBackPressedCallback(this)
        delegate.hostDestroyed()
    }

    internal fun getRouter(
        container: ViewGroup,
        controllerFactory: ControllerFactory?
    ): Router = delegate.getRouter(container, controllerFactory)

    override fun handleOnBackPressed(): Boolean = delegate.handleBack()

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