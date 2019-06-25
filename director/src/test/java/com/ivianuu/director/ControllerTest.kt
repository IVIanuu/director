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

import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.router(activityProxy.view1).apply {
        if (!hasRoot) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testChildControllerParent() {
        val parent = TestController()
        val child = TestController()

        router.push(parent.toTransaction())
        parent.childRouter(parent.childContainer1!!)
            .push(child.toTransaction())

        assertEquals(parent, child.parentController)
    }

    @Test
    fun testAttachHostAwareness() {
        val controller = TestController()
        router.push(controller.toTransaction())

        assertTrue(controller.lifecycle.currentState == RESUMED)
        router.stop()
        assertFalse(controller.lifecycle.currentState == RESUMED)
        router.start()
        assertTrue(controller.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testControllerState() {
        val controller = TestController()

        assertEquals(INITIALIZED, controller.lifecycle.currentState)

        controller.addLifecycleListener(
            preCreate = { assertEquals(INITIALIZED, controller.lifecycle.currentState) },
            postCreate = { assertEquals(CREATED, controller.lifecycle.currentState) },
            preAttach = { _, _ -> assertEquals(CREATED, controller.lifecycle.currentState) },
            postAttach = { _, _ -> assertEquals(RESUMED, controller.lifecycle.currentState) },
            preDetach = { _, _ -> assertEquals(RESUMED, controller.lifecycle.currentState) },
            postDetach = { _, _ -> assertEquals(CREATED, controller.lifecycle.currentState) },
            preDestroy = { assertEquals(CREATED, controller.lifecycle.currentState) },
            postDestroy = { assertEquals(DESTROYED, controller.lifecycle.currentState) }
        )

        router.push(controller.toTransaction())
        controller.doOnPostAttach { _, _ -> router.popTop() }
    }

}