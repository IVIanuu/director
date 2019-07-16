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
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.TestController
import com.ivianuu.director.util.defaultHandler
import com.ivianuu.director.util.noRemoveViewOnPushHandler
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerReattachTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.router(activityProxy.view1).apply {
        if (!hasRoot) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testNeedsAttachOnPause() {
        val controllerA = TestController()
        val controllerB = TestController()

        router.push(
            controllerA
                .toTransaction()
                .changeHandler(defaultHandler())
        )

        assertTrue(controllerA.lifecycle.currentState == RESUMED)
        assertFalse(controllerB.lifecycle.currentState == RESUMED)

        sleepWakeDevice()

        assertTrue(controllerA.lifecycle.currentState == RESUMED)
        assertFalse(controllerB.lifecycle.currentState == RESUMED)

        router.push(
            controllerB
                .toTransaction()
                .changeHandler(defaultHandler())
        )

        assertFalse(controllerA.lifecycle.currentState == RESUMED)
        assertTrue(controllerB.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testPopMiddleControllerAttaches() {
        var controller1: Controller = TestController()
        var controller2: Controller = TestController()
        var controller3: Controller = TestController()

        router.setRoot(controller1.toTransaction())
        router.push(controller2.toTransaction())
        router.push(controller3.toTransaction())
        router.popController(controller2)

        assertFalse(controller1.lifecycle.currentState == RESUMED)
        assertFalse(controller2.lifecycle.currentState == RESUMED)
        assertTrue(controller3.lifecycle.currentState == RESUMED)

        controller1 = TestController()
        controller2 = TestController()
        controller3 = TestController()

        router.setRoot(controller1.toTransaction())
        router.push(controller2.toTransaction())
        router.push(
            controller3
                .toTransaction()
                .pushChangeHandler(noRemoveViewOnPushHandler())
        )
        router.popController(controller2)

        assertTrue(controller1.lifecycle.currentState == RESUMED)
        assertFalse(controller2.lifecycle.currentState == RESUMED)
        assertTrue(controller3.lifecycle.currentState == RESUMED)
    }

    private fun sleepWakeDevice() {
        activityProxy.saveInstanceState(Bundle()).pause()
        activityProxy.resume()
    }
}