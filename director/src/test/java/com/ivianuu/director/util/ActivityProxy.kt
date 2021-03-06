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

    val view1 = AttachFakingFrameLayout(activityController.get()).apply {
        id = 4
    }

    val view2 = AttachFakingFrameLayout(activityController.get()).apply {
        id = 5
    }

    val activity: TestActivity
        get() = activityController.get()

    fun create(savedInstanceState: Bundle?): ActivityProxy = apply {
        activityController.create(savedInstanceState)
        view1.setAttached(true)
        view2.setAttached(true)
    }

    fun start(): ActivityProxy = apply {
        activityController.start()
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

    fun stop(): ActivityProxy = apply {
        activityController.stop()
    }

    fun destroy(): ActivityProxy = apply {
        activityController.destroy()
        view1.setAttached(false)
        view2.setAttached(false)
    }

}