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
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerViewRetentionTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1).apply {
        if (!hasRoot) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testViewRetentionWithRetainViewOff() {
        var controller = TestController()
        var onTopController = TestController()

        // without retain view
        controller.retainView = false
        assertNull(controller.view)
        router.push(controller.toTransaction())
        assertNotNull(controller.view)
        router.pop(controller)
        assertNull(controller.view)

        // with retain view
        controller = TestController()
        onTopController = TestController()
        controller.retainView = true
        assertNull(controller.view)
        router.push(controller.toTransaction())
        assertNotNull(controller.view)
        router.push(onTopController.toTransaction())
        assertNotNull(controller.view)

        // disable retain should release the view
        controller.retainView = false
        assertNull(controller.view)
    }

    @Test
    fun testChildViewRetention() {
        val parent = TestController()
        parent.retainView = false

        val child1 = TestController()
        child1.retainView = true

        val child2 = TestController()
        child2.retainView = false

        val child3 = TestController()
        child3.retainView = true

        router.setRoot(parent.toTransaction())

        parent.getChildRouter(parent.childContainer1!!)
            .setRoot(child1.toTransaction())

        child1.getChildRouter(child1.childContainer1!!)
            .setRoot(child2.toTransaction())

        child2.getChildRouter(child2.childContainer1!!)
            .setRoot(child3.toTransaction())

        assertTrue(parent.isAttached)
        assertTrue(child1.isAttached)
        assertTrue(child2.isAttached)
        assertTrue(child3.isAttached)

        val parentView = parent.view
        val child1View = child1.view
        val child2View = child2.view
        val child3View = child3.view

        val onTopController = TestController()

        router.push(onTopController.toTransaction())

        assertNull(parent.view)
        assertNotNull(child1.view)
        assertNull(child2.view)
        assertNotNull(child3.view)

        assertFalse(parent.isAttached)
        assertFalse(child1.isAttached)
        assertFalse(child2.isAttached)
        assertFalse(child3.isAttached)

        // retained views should remove their views from parents
        assertNull(child1View!!.parent)
        assertNull(child3View!!.parent)

        router.popCurrent()

        assertNotEquals(parentView, parent.view)
        assertEquals(child1View, child1.view)
        assertNotEquals(child2View, child2.view)
        assertEquals(child3View, child3.view)

        assertTrue(parent.isAttached)
        assertTrue(child1.isAttached)
        assertTrue(child2.isAttached)
        assertTrue(child3.isAttached)
    }

}