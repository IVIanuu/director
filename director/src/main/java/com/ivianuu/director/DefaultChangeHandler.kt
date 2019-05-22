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

package com.ivianuu.director

import android.os.Bundle

/**
 * A [ControllerChangeHandler] that will instantly swap Views with no animations or transitions.
 */
open class DefaultChangeHandler(
    removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
) : ControllerChangeHandler() {

    override val removesFromViewOnPush: Boolean get() = _removesFromViewOnPush
    private var _removesFromViewOnPush = removesFromViewOnPush

    override fun performChange(changeData: ChangeData) {
        changeData.callback.addToView()
        changeData.callback.removeFromView()
        changeData.callback.onChangeCompleted()
    }

    override fun saveInstanceState(outState: Bundle) {
        super.saveInstanceState(outState)
        outState.putBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH, _removesFromViewOnPush)
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        super.restoreInstanceState(savedInstanceState)
        _removesFromViewOnPush = savedInstanceState.getBoolean(KEY_REMOVES_FROM_VIEW_ON_PUSH)
    }

    companion object {
        private const val KEY_REMOVES_FROM_VIEW_ON_PUSH =
            "DefaultChangeHandler.removesFromViewOnPush"
    }
}