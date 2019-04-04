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

package com.ivianuu.director.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller

class TestController : Controller() {

    var currentCallState = CallState()

    val childContainer1: ViewGroup? get() = view?.findViewById(CHILD_VIEW_ID_1)
    val childContainer2: ViewGroup? get() = view?.findViewById(CHILD_VIEW_ID_2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentCallState = savedInstanceState.getParcelable(KEY_CALL_STATE)!!
        }
        currentCallState.createCalls++
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        currentCallState.createViewCalls++
        val view = AttachFakingFrameLayout(inflater.context)
        view.id = VIEW_ID

        val childContainer1 = AttachFakingFrameLayout(inflater.context)
        childContainer1.id = CHILD_VIEW_ID_1
        view.addView(childContainer1)

        val childContainer2 = AttachFakingFrameLayout(inflater.context)
        childContainer2.id = CHILD_VIEW_ID_2
        view.addView(childContainer2)

        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        currentCallState.attachCalls++
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        currentCallState.detachCalls++
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        currentCallState.destroyViewCalls++
    }

    override fun onDestroy() {
        super.onDestroy()
        currentCallState.destroyCalls++
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        currentCallState.restoreViewStateCalls++
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        currentCallState.saveViewStateCalls++
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentCallState.restoreInstanceStateCalls++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentCallState.saveInstanceStateCalls++
        outState.putParcelable(KEY_CALL_STATE, currentCallState)
    }

    companion object {
        private const val VIEW_ID = 2342
        const val CHILD_VIEW_ID_1 = 2343
        const val CHILD_VIEW_ID_2 = 2344

        private const val KEY_CALL_STATE = "TestController.currentCallState"
    }
}