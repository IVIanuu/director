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
import com.ivianuu.director.ChangeHandler
import com.ivianuu.director.internal.moveView

private object NoopListener : MockChangeHandler.Listener

class MockChangeHandler internal constructor(
    override var removesFromViewOnPush: Boolean = true,
    var tag: String? = null,
    val listener: Listener = NoopListener
) : ChangeHandler() {

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
            callback, toIndex) = changeData

        this.from = from
        this.to = to

        listener.willStartChange()

        if (isPush) {
            if (to != null) {
                if (to.parent == null) {
                    container.addView(to, toIndex)
                    listener.didAttachOrDetach()
                } else if (container.indexOfChild(to) != toIndex) {
                    container.moveView(to, toIndex)
                }
            }

            if ((removesFromViewOnPush || changeData.forceRemoveFromViewOnPush) && from != null) {
                container.removeView(from)
            }
        } else {
            container.removeView(from)
            listener.didAttachOrDetach()

            if (to != null) {
                container.addView(to)
            }
        }

        callback.onChangeCompleted()
        listener.didEndChange()
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH, removesFromViewOnPush)
        bundle.putString(KEY_TAG, tag)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        removesFromViewOnPush = bundle.getBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH)
        tag = bundle.getString(KEY_TAG)
    }

    override fun copy(): ChangeHandler = MockChangeHandler(removesFromViewOnPush, tag, listener)

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