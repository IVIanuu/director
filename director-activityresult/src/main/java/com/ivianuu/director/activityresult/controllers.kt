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

package com.ivianuu.director.activityresult

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import com.ivianuu.director.Controller
import com.ivianuu.director.doOnPostDestroy

/**
 * Notifies the [listener] on activity results for [requestCode]
 */
fun Controller.registerActivityResultListener(
    requestCode: Int,
    listener: ActivityResultListener
) {
    // remove the listener on controller destruction
    doOnPostDestroy {
        ActivityResultHandler.get(this)
            .unregisterListener(requestCode, listener)
    }

    ActivityResultHandler.get(this).registerListener(requestCode, listener)
}

/**
 * Invokes [onActivityResult] on activity results for [requestCode]
 */
fun Controller.registerActivityResultListener(
    requestCode: Int,
    onActivityResult: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit
): ActivityResultListener {
    val listener = object : ActivityResultListener {
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            onActivityResult(requestCode, resultCode, data)
        }
    }

    registerActivityResultListener(requestCode, listener)

    return listener
}

/**
 * Unregisters the previously added [listener]
 */
fun Controller.unregisterActivityResultListener(
    requestCode: Int,
    listener: ActivityResultListener
) {
    ActivityResultHandler.get(this).unregisterListener(requestCode, listener)
}

/**
 * Starts the intent for result
 */
fun Controller.startActivityForResult(
    intent: Intent,
    requestCode: Int,
    options: Bundle? = null
) {
    ActivityResultHandler.get(this).startActivityForResult(intent, requestCode, options)
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
    ActivityResultHandler.get(this).startIntentSenderForResult(
        intent,
        requestCode,
        fillInIntent,
        flagsMask,
        flagsValues,
        extraFlags,
        options
    )
}