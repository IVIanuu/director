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

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CallState(
    var createCalls: Int = 0,
    var destroyCalls: Int = 0,

    var createViewCalls: Int = 0,
    var destroyViewCalls: Int = 0,

    var attachCalls: Int = 0,
    var detachCalls: Int = 0
) : Parcelable