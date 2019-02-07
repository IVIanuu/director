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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerViewRetentionTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getOrCreateRouter(activityProxy.view).apply {
        if (!hasRootController) {
            setRoot(TestController())
        }
    }

    @Test
    fun testViewRetentionWithRetainViewOff() {
        var controller = TestController()
        var onTopController = TestController()

        // without retain view
        controller.retainView = false
        assertNull(controller.view)
        router.push(controller)
        assertNotNull(controller.view)
        router.pop(controller)
        assertNull(controller.view)

        // with retain view
        controller = TestController()
        onTopController = TestController()
        controller.retainView = true
        assertNull(controller.view)
        router.push(controller)
        assertNotNull(controller.view)
        router.push(onTopController)
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

        router.setRoot(parent)

        parent.getChildRouter(parent.childContainer1!!)
            .setRoot(child1)

        child1.getChildRouter(child1.childContainer1!!)
            .setRoot(child2)

        child2.getChildRouter(child2.childContainer1!!)
            .setRoot(child3)

        val parentView = parent.view
        val child1View = child1.view
        val child2View = child2.view
        val child3View = child3.view

        val onTopController = TestController()

        router.push(onTopController)

        assertNull(parent.view)
        assertNotNull(child1.view)
        assertNull(child2.view)
        assertNotNull(child3.view)

        router.popCurrent()

        assertNotEquals(parentView, parent.view)
        assertEquals(child1View, child1.view)
        assertNotEquals(child2View, child2.view)
        assertEquals(child3View, child3.view)
    }

    @Test
    fun testChildViewRetentionOnDetach() {
        val parent = TestController()
        parent.retainView = false

        val child1 = TestController()
        child1.retainView = true

        router.setRoot(parent)

        parent.getChildRouter(parent.childContainer1!!)
            .setRoot(child1)

        val parentView = parent.view
        val child1View = child1.view

        activityProxy.stop(true)

        assertNull(parent.view)
        assertNotNull(child1.view)

        activityProxy.resume()

        assertNotEquals(parentView, parent.view)
        assertEquals(child1View, child1.view)
    }
}