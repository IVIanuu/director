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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.MockChangeHandler
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TargetControllerTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1).apply {
        if (!hasRoot) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testSiblingTarget() {
        val controllerA = TestController()
        val controllerB = TestController()

        assertNull(controllerA.targetController)
        assertNull(controllerB.targetController)

        router.push(
            controllerA.toTransaction()
                .changeHandler(MockChangeHandler.defaultHandler())
        )

        controllerB.targetController = controllerA

        router.push(
            controllerB.toTransaction()
                .changeHandler(MockChangeHandler.defaultHandler())
        )

        assertNull(controllerA.targetController)
        assertEquals(controllerA, controllerB.targetController)
    }

    @Test
    fun testParentChildTarget() {
        val controllerA = TestController()
        val controllerB = TestController()

        assertNull(controllerA.targetController)
        assertNull(controllerB.targetController)

        router.push(
            controllerA.toTransaction()
                .changeHandler(MockChangeHandler.defaultHandler())
        )

        controllerB.targetController = controllerA

        val childRouter =
            controllerA.getChildRouter(controllerA.childContainer1!!)
        childRouter.push(
            controllerB.toTransaction()
                .changeHandler(MockChangeHandler.defaultHandler())
        )

        assertNull(controllerA.targetController)
        assertEquals(controllerA, controllerB.targetController)
    }

    @Test
    fun testChildParentTarget() {
        val controllerA = TestController()
        val controllerB = TestController()

        assertNull(controllerA.targetController)
        assertNull(controllerB.targetController)

        router.push(
            controllerA.toTransaction()
                .changeHandler(MockChangeHandler.defaultHandler())
        )

        controllerA.targetController = controllerB

        val childRouter =
            controllerA.getChildRouter(
                controllerA.childContainer1!!
            )

        childRouter.push(
            controllerB.toTransaction()
                .changeHandler(MockChangeHandler.defaultHandler())
        )

        assertNull(controllerB.targetController)
        assertEquals(controllerB, controllerA.targetController)
    }
}