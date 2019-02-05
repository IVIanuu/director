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
    private val router = activityProxy.activity.attachRouter(activityProxy.view).apply {
        if (!hasRootController) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testNeedsAttachOnPause() {
        val controllerA = TestController()
        val controllerB = TestController()

        router.pushController(
            controllerA.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertTrue(controllerA.state == ControllerState.ATTACHED)
        assertFalse(controllerB.state == ControllerState.ATTACHED)

        sleepWakeDevice()

        assertTrue(controllerA.state == ControllerState.ATTACHED)
        assertFalse(controllerB.state == ControllerState.ATTACHED)

        router.pushController(
            controllerB.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertFalse(controllerA.state == ControllerState.ATTACHED)
        assertTrue(controllerB.state == ControllerState.ATTACHED)
    }

    @Test
    fun testChildNeedsAttachOnPause() {
        val controllerA = TestController()
        val childController = TestController()
        val controllerB = TestController()

        router.pushController(
            controllerA.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        val childRouter =
            controllerA.getChildRouter(controllerA.childContainer1!!)
        childRouter.pushController(
            childController.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertTrue(controllerA.state == ControllerState.ATTACHED)
        assertTrue(childController.state == ControllerState.ATTACHED)
        assertFalse(controllerB.state == ControllerState.ATTACHED)

        sleepWakeDevice()

        assertTrue(controllerA.state == ControllerState.ATTACHED)
        assertTrue(childController.state == ControllerState.ATTACHED)
        assertFalse(controllerB.state == ControllerState.ATTACHED)

        router.pushController(
            controllerB.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertFalse(controllerA.state == ControllerState.ATTACHED)
        assertFalse(childController.state == ControllerState.ATTACHED)
        assertTrue(controllerB.state == ControllerState.ATTACHED)
    }

    @Test
    fun testChildHandleBack() {
        val controllerA = TestController()
        val controllerB = TestController()
        val childController = TestController()

        router.pushController(
            controllerA.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertTrue(controllerA.state == ControllerState.ATTACHED)
        assertFalse(controllerB.state == ControllerState.ATTACHED)
        assertFalse(childController.state == ControllerState.ATTACHED)

        router.pushController(
            controllerB.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        val childRouter =
            controllerB.getChildRouter(controllerB.childContainer1!!)
                .popsLastView(true)

        childRouter.pushController(
            childController.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertFalse(controllerA.state == ControllerState.ATTACHED)
        assertTrue(controllerB.state == ControllerState.ATTACHED)
        assertTrue(childController.state == ControllerState.ATTACHED)
    }

    // Attempt to test https://github.com/bluelinelabs/Conductor/issues/86#issuecomment-231381271
    @Test
    fun testReusedChildRouterHandleBack() {
        val controllerA = TestController()
        val controllerB = TestController()
        var childController = TestController()

        router.pushController(
            controllerA.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertTrue(controllerA.state == ControllerState.ATTACHED)
        assertFalse(controllerB.state == ControllerState.ATTACHED)
        assertFalse(childController.state == ControllerState.ATTACHED)

        router.pushController(
            controllerB.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        val childRouter =
            controllerB.getChildRouter(controllerB.childContainer1!!)
                .popsLastView(true)

        childRouter.pushController(
            childController.toTransaction()
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler())
        )

        assertFalse(controllerA.state == ControllerState.ATTACHED)
        assertTrue(controllerB.state == ControllerState.ATTACHED)
        assertTrue(childController.state == ControllerState.ATTACHED)

        router.handleBack()

        assertFalse(controllerA.state == ControllerState.ATTACHED)
        assertTrue(controllerB.state == ControllerState.ATTACHED)
        assertFalse(childController.state == ControllerState.ATTACHED)

        childController = TestController()
        childRouter.pushController(
            childController.toTransaction()
                .changeHandler(MockChangeHandler.defaultHandler())
        )

        assertFalse(controllerA.state == ControllerState.ATTACHED)
        assertTrue(controllerB.state == ControllerState.ATTACHED)
        assertTrue(childController.state == ControllerState.ATTACHED)
    }

    @Test
    fun testPopMiddleControllerAttaches() {
        var controller1: Controller = TestController()
        var controller2: Controller = TestController()
        var controller3: Controller = TestController()

        router.setRoot(controller1.toTransaction())
        router.pushController(controller2.toTransaction())
        router.pushController(controller3.toTransaction())
        router.popController(controller2)

        assertFalse(controller1.state == ControllerState.ATTACHED)
        assertFalse(controller2.state == ControllerState.ATTACHED)
        assertTrue(controller3.state == ControllerState.ATTACHED)

        controller1 = TestController()
        controller2 = TestController()
        controller3 = TestController()

        router.setRoot(controller1.toTransaction())
        router.pushController(controller2.toTransaction())
        router.pushController(
            controller3.toTransaction()
                .pushChangeHandler(
                    MockChangeHandler.noRemoveViewOnPushHandler()
                )
        )
        router.popController(controller2)

        assertTrue(controller1.state == ControllerState.ATTACHED)
        assertFalse(controller2.state == ControllerState.ATTACHED)
        assertTrue(controller3.state == ControllerState.ATTACHED)
    }

    private fun sleepWakeDevice() {
        activityProxy.saveInstanceState(Bundle()).pause()
        activityProxy.resume()
    }
}