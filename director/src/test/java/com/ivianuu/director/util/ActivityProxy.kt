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
import org.robolectric.Robolectric

class ActivityProxy {

    private val activityController = Robolectric.buildActivity(TestActivity::class.java)

    val view = AttachFakingFrameLayout(activityController.get()).apply {
        id = 4
    }

    val activity: TestActivity
        get() = activityController.get()

    fun create(savedInstanceState: Bundle?): ActivityProxy = apply {
        activityController.create(savedInstanceState)
    }

    fun start(): ActivityProxy = apply {
        activityController.start()
        view.setAttached(true)
    }

    fun resume(): ActivityProxy = apply {
        activityController.resume()
    }

    fun pause(): ActivityProxy = apply {
        activityController.pause()
    }

    fun saveInstanceState(outState: Bundle): ActivityProxy = apply {
        activityController.saveInstanceState(outState)
    }

    fun stop(detachView: Boolean): ActivityProxy = apply {
        activityController.stop()

        if (detachView) {
            view.setAttached(false)
        }
    }

    fun destroy(): ActivityProxy = apply {
        activityController.destroy()
        view.setAttached(false)
    }

    fun rotate(): ActivityProxy = apply {
        activity.changingConfigurations = true
        activity.recreate()
    }
}