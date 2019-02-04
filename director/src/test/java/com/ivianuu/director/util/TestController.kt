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
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType

class TestController : Controller() {

    var currentCallState = CallState()
    var changeHandlerHistory = ChangeHandlerHistory()

    val childContainer1: ViewGroup? get() = view?.findViewById(CHILD_VIEW_ID_1)
    val childContainer2: ViewGroup? get() = view?.findViewById(CHILD_VIEW_ID_2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentCallState = savedInstanceState.getParcelable(KEY_CALL_STATE)!!
        }
        currentCallState.createCalls++
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        currentCallState.inflateViewCalls++
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

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        currentCallState.bindViewCalls++
    }

    override fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeStarted(changeHandler, changeType)
        currentCallState.changeStartCalls++
    }

    override fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(changeHandler, changeType)
        currentCallState.changeEndCalls++

        if (changeHandler is MockChangeHandler) {
            changeHandlerHistory.addEntry(
                changeHandler.from,
                changeHandler.to,
                changeType.isPush,
                changeHandler
            )
        } else {
            changeHandlerHistory.isValidHistory = false
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        currentCallState.attachCalls++
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        currentCallState.detachCalls++
    }

    override fun onUnbindView(view: View) {
        super.onUnbindView(view)
        currentCallState.unbindViewCalls++
    }

    override fun onDestroy() {
        super.onDestroy()
        currentCallState.destroyCalls++
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        currentCallState.saveViewStateCalls++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        currentCallState.saveInstanceStateCalls++
        outState.putParcelable(KEY_CALL_STATE, currentCallState)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val VIEW_ID = 2342
        private const val CHILD_VIEW_ID_1 = 2343
        private const val CHILD_VIEW_ID_2 = 2344

        private const val KEY_CALL_STATE = "TestController.currentCallState"
    }
}