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

package com.ivianuu.director.permission

import android.app.Activity
import android.os.Build

/**
 * Permission callback
 */
interface PermissionCallback {

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )

    fun shouldShowRequestPermissionRationale(
        activity: Activity,
        permission: String
    ): Boolean = Build.VERSION.SDK_INT >= 23 &&
            activity.shouldShowRequestPermissionRationale(permission)

}