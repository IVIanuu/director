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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.ControllerState.ATTACHED
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.MockChangeHandler
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ReattachTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1).apply {
        if (!hasRootController) {
            setRoot(TestController())
        }
    }

    @Test
    fun testNeedsAttachOnPause() {
        val controllerA = TestController()
        val controllerB = TestController()

        router.push(
            controllerA,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertTrue(controllerA.state == ATTACHED)
        assertFalse(controllerB.state == ATTACHED)

        sleepWakeDevice()

        assertTrue(controllerA.state == ATTACHED)
        assertFalse(controllerB.state == ATTACHED)

        router.push(
            controllerB,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertFalse(controllerA.state == ATTACHED)
        assertTrue(controllerB.state == ATTACHED)
    }

    @Test
    fun testChildNeedsAttachOnPause() {
        val controllerA = TestController()
        val childController = TestController()
        val controllerB = TestController()

        router.push(
            controllerA,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        val childRouter =
            controllerA.getChildRouter(controllerA.childContainer1!!)
        childRouter.push(
            childController,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertTrue(controllerA.state == ATTACHED)
        assertTrue(childController.state == ATTACHED)
        assertFalse(controllerB.state == ATTACHED)

        sleepWakeDevice()

        assertTrue(controllerA.state == ATTACHED)
        assertTrue(childController.state == ATTACHED)
        assertFalse(controllerB.state == ATTACHED)

        router.push(
            controllerB,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertFalse(controllerA.state == ATTACHED)
        assertFalse(childController.state == ATTACHED)
        assertTrue(controllerB.state == ATTACHED)
    }

    @Test
    fun testChildHandleBack() {
        val controllerA = TestController()
        val controllerB = TestController()
        val childController = TestController()

        router.push(
            controllerA,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertTrue(controllerA.state == ATTACHED)
        assertFalse(controllerB.state == ATTACHED)
        assertFalse(childController.state == ATTACHED)

        router.push(
            controllerB,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        val childRouter =
            controllerB.getChildRouter(controllerB.childContainer1!!)
                .popsLastView(true)

        childRouter.push(
            childController,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertFalse(controllerA.state == ATTACHED)
        assertTrue(controllerB.state == ATTACHED)
        assertTrue(childController.state == ATTACHED)
    }

    // Attempt to test https://github.com/bluelinelabs/Conductor/issues/86#issuecomment-231381271
    @Test
    fun testReusedChildRouterHandleBack() {
        val controllerA = TestController()
        val controllerB = TestController()
        var childController = TestController()

        router.push(
            controllerA,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertTrue(controllerA.state == ATTACHED)
        assertFalse(controllerB.state == ATTACHED)
        assertFalse(childController.state == ATTACHED)

        router.push(
            controllerB,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        val childRouter =
            controllerB.getChildRouter(controllerB.childContainer1!!)
                .popsLastView(true)

        childRouter.push(
            childController,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertFalse(controllerA.state == ATTACHED)
        assertTrue(controllerB.state == ATTACHED)
        assertTrue(childController.state == ATTACHED)

        router.handleBack()

        assertFalse(controllerA.state == ATTACHED)
        assertTrue(controllerB.state == ATTACHED)
        assertFalse(childController.state == ATTACHED)

        childController = TestController()
        childRouter.push(
            childController,
            MockChangeHandler.defaultHandler(),
            MockChangeHandler.defaultHandler()
        )

        assertFalse(controllerA.state == ATTACHED)
        assertTrue(controllerB.state == ATTACHED)
        assertTrue(childController.state == ATTACHED)
    }

    @Test
    fun testPopMiddleControllerAttaches() {
        var controller1: Controller = TestController()
        var controller2: Controller = TestController()
        var controller3: Controller = TestController()

        router.setRoot(controller1)
        router.push(controller2)
        router.push(controller3)
        router.pop(controller2)

        assertFalse(controller1.state == ATTACHED)
        assertFalse(controller2.state == ATTACHED)
        assertTrue(controller3.state == ATTACHED)

        controller1 = TestController()
        controller2 = TestController()
        controller3 = TestController()

        router.setRoot(controller1)
        router.push(controller2)
        router.push(controller3, MockChangeHandler.noRemoveViewOnPushHandler())
        router.pop(controller2)

        assertTrue(controller1.state == ATTACHED)
        assertFalse(controller2.state == ATTACHED)
        assertTrue(controller3.state == ATTACHED)
    }

    private fun sleepWakeDevice() {
        activityProxy.saveInstanceState(Bundle()).pause()
        activityProxy.resume()
    }
}