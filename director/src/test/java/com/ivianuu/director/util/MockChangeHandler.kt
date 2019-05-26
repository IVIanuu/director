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
import android.view.View
import com.ivianuu.director.ChangeData
import com.ivianuu.director.ControllerChangeHandler

private object NoopListener : MockChangeHandler.Listener

class MockChangeHandler internal constructor(
    override var removesFromViewOnPush: Boolean = true,
    var tag: String? = null,
    val listener: Listener = NoopListener
) : ControllerChangeHandler() {

    var from: View? = null
    var to: View? = null

    interface Listener {
        fun willStartChange() {
        }

        fun didAttachOrDetach() {
        }

        fun didEndChange() {
        }
    }

    override fun performChange(changeData: ChangeData) {
        val (container, from, to, isPush,
            callback) = changeData

        this.from = from
        this.to = to

        listener.willStartChange()

        changeData.callback.addToView()
        changeData.callback.removeFromView()
        if (changeData.from != null || changeData.to != null) {
            listener.didAttachOrDetach()
        }

        callback.changeCompleted()
        listener.didEndChange()
    }

    override fun saveToBundle(outState: Bundle) {
        super.saveToBundle(outState)
        outState.putBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH, removesFromViewOnPush)
        outState.putString(KEY_TAG, tag)
    }

    override fun restoreFromBundle(savedInstanceState: Bundle) {
        super.restoreFromBundle(savedInstanceState)
        removesFromViewOnPush = savedInstanceState.getBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH)
        tag = savedInstanceState.getString(KEY_TAG)
    }

    override fun copy(): ControllerChangeHandler = MockChangeHandler(removesFromViewOnPush, tag, listener)

    companion object {
        private const val KEY_REMOVES_FROM_VIEW_ON_PUSH = "removesFromViewOnPush"
        private const val KEY_TAG = "tag"
    }
}

fun defaultHandler(): MockChangeHandler = MockChangeHandler(true, null)

fun noRemoveViewOnPushHandler(): MockChangeHandler = MockChangeHandler(
    false,
    null
)

fun noRemoveViewOnPushHandler(tag: String): MockChangeHandler =
    MockChangeHandler(false, tag)

fun listeningChangeHandler(listener: MockChangeHandler.Listener): MockChangeHandler =
    MockChangeHandler(true, null, listener)

fun taggedHandler(tag: String, removeViewOnPush: Boolean): MockChangeHandler =
    MockChangeHandler(removeViewOnPush, tag)