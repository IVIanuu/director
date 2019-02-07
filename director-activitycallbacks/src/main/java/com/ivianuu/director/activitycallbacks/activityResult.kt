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

package com.ivianuu.director.activitycallbacks

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import com.ivianuu.director.Controller
import com.ivianuu.director.doOnPostDestroy

/**
 * Listener for activity results
 */
interface ActivityResultListener {
    /**
     * Will be called on activity results
     */
    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    )
}

/**
 * Notifies the [listener] on activity results for [requestCode]
 */
fun Controller.addActivityResultListener(
    requestCode: Int,
    listener: ActivityResultListener
) {
    // remove the listener on controller destruction
    doOnPostDestroy {
        activityCallbacks
            .removeActivityResultListener(requestCode, listener)
    }

    activityCallbacks.addActivityResultListener(requestCode, listener)
}

/**
 * Invokes [onActivityResult] on activity results for [requestCode]
 */
fun Controller.addActivityResultListener(
    requestCode: Int,
    onActivityResult: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit
): ActivityResultListener {
    val listener = object : ActivityResultListener {
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            onActivityResult(requestCode, resultCode, data)
        }
    }

    addActivityResultListener(requestCode, listener)

    return listener
}

/**
 * Removes the previously added [listener]
 */
fun Controller.removeActivityResultListener(
    requestCode: Int,
    listener: ActivityResultListener
) {
    activityCallbacks.removeActivityResultListener(requestCode, listener)
}

/**
 * Starts the intent for result
 */
fun Controller.startActivityForResult(
    intent: Intent,
    requestCode: Int,
    options: Bundle? = null
) {
    activityCallbacks.startActivityForResult(intent, requestCode, options)
}

/**
 * Starts the intent sender for result
 */
fun Controller.startIntentSenderForResult(
    intent: IntentSender,
    requestCode: Int,
    fillInIntent: Intent?,
    flagsMask: Int,
    flagsValues: Int,
    extraFlags: Int,
    options: Bundle? = null
) {
    activityCallbacks.startIntentSenderForResult(
        intent,
        requestCode,
        fillInIntent,
        flagsMask,
        flagsValues,
        extraFlags,
        options
    )
}
