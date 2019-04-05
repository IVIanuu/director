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

import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.ControllerState.ATTACHED
import com.ivianuu.director.ControllerState.CREATED
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.ControllerState.INITIALIZED
import com.ivianuu.director.ControllerState.VIEW_CREATED
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.TestController
import com.ivianuu.director.util.reportAttached
import com.ivianuu.director.util.setParent
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
    private val router = activityProxy.activity.getRouter(activityProxy.view1).apply {
        if (!hasRoot) {
            setRoot(TestController())
        }
    }

    @Test
    fun testChildControllerParent() {
        val parent = TestController()
        val child = TestController()

        router.push(parent)
        parent.getChildRouter(parent.childContainer1!!)
            .push(child)

        assertEquals(parent, child.parentController)
    }

    @Test
    fun testAttachedToUnownedParent() {
        val controller = TestController()

        controller.doOnPostCreateView { _, view, _ ->
            view.setParent(FrameLayout(view.context))
        }

        router.push(controller)

        assertFalse(controller.isAttached)
        controller.view!!.setParent(router.container)
        controller.view!!.reportAttached(true)
        assertTrue(controller.isAttached)
    }

    @Test
    fun testAttachHostAwareness() {
        val controller = TestController()
        router.push(controller)

        assertTrue(controller.isAttached)
        router.hostStopped()
        assertFalse(controller.isAttached)
        router.onStart()
        assertTrue(controller.isAttached)
    }

    @Test
    fun testControllerState() {
        val controller = TestController()

        assertEquals(INITIALIZED, controller.state)

        controller.addListener(
            preCreate = { _, _ -> assertEquals(INITIALIZED, controller.state) },
            postCreate = { _, _ -> assertEquals(CREATED, controller.state) },
            preCreateView = { _, _ -> assertEquals(CREATED, controller.state) },
            postCreateView = { _, _, _ -> assertEquals(VIEW_CREATED, controller.state) },
            preAttach = { _, _ -> assertEquals(VIEW_CREATED, controller.state) },
            postAttach = { _, _ -> assertEquals(ATTACHED, controller.state) },
            preDetach = { _, _ -> assertEquals(ATTACHED, controller.state) },
            postDetach = { _, _ -> assertEquals(VIEW_CREATED, controller.state) },
            preDestroyView = { _, _ -> assertEquals(VIEW_CREATED, controller.state) },
            postDestroyView = { assertEquals(CREATED, controller.state) },
            preDestroy = { assertEquals(CREATED, controller.state) },
            postDestroy = { assertEquals(DESTROYED, controller.state) }
        )

        router.push(controller)
        controller.doOnPostAttach { _, _ -> router.popTop() }
    }

}