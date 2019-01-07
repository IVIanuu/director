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

import android.annotation.TargetApi
import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup

/**
 * The application of the attached activity
 */
val Controller.application: Application
    get() = activity.application

/**
 * The resources of the attached activity
 */
val Controller.resources: Resources get() = activity.resources

/**
 * Returns a new router transaction
 */
fun Controller.toTransaction(): RouterTransaction = RouterTransaction(this)

/**
 * Calls startActivity(Intent) from this Controller's host Activity.
 */
fun Controller.startActivity(intent: Intent) {
    router.startActivity(intent)
}

/**
 * Calls startActivityForResult(Intent, int) from this Controller's host Activity.
 */
fun Controller.startActivityForResult(intent: Intent, requestCode: Int) {
    router.startActivityForResult(instanceId, intent, requestCode)
}

/**
 * Calls startActivityForResult(Intent, int, Bundle) from this Controller's host Activity.
 */
fun Controller.startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
    router.startActivityForResult(instanceId, intent, requestCode, options)
}

/**
 * Calls startIntentSenderForResult(IntentSender, int, Intent, int, int, int, Bundle) from this Controller's host Activity.
 */
fun Controller.startIntentSenderForResult(
    intent: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int,
    flagsValues: Int, extraFlags: Int, options: Bundle?
) {
    router.startIntentSenderForResult(
        instanceId,
        intent,
        requestCode,
        fillInIntent,
        flagsMask,
        flagsValues,
        extraFlags,
        options
    )
}

/**
 * Registers this Controller to handle [Controller.onActivityResult] responses. Calling this method is NOT
 * necessary when calling [startActivityForResult]
 */
fun Controller.registerForActivityResult(requestCode: Int) {
    router.registerForActivityResult(instanceId, requestCode)
}

/**
 * Calls requestPermission(String[], int) from this Controller's host Activity. Results for this request,
 * including [.shouldShowRequestPermissionRationale] and
 * [.onRequestPermissionsResult] will be forwarded back to this Controller by the system.
 */
@TargetApi(Build.VERSION_CODES.M)
fun Controller.requestPermissions(permissions: Array<String>, requestCode: Int) {
    router.requestPermissions(instanceId, permissions, requestCode)
}

/**
 * Returns the child router for [container] and [tag] or creates a new instance
 */
fun Controller.getChildRouter(container: ViewGroup, tag: String? = null) =
    getChildRouter(container.id, tag)

/**
 * Returns the child router for [container] and [tag] if already created
 */
fun Controller.getChildRouterOrNull(container: ViewGroup, tag: String? = null) =
    getChildRouter(container.id, tag)