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
import com.ivianuu.director.util.ViewUtils
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
    private val router = activityProxy.activity.attachRouter(activityProxy.view).apply {
        if (!hasRootController) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testViewRetention() {
        val controller = TestController()
        controller.setRouter(router)

        // Test View getting released w/ RELEASE_DETACH
        controller.retainView = false
        assertNull(controller.view)
        var view = controller.inflate(router.container!!)
        assertNotNull(controller.view)
        ViewUtils.reportAttached(view, true)
        assertNotNull(controller.view)
        ViewUtils.reportAttached(view, false)
        assertNull(controller.view)

        // Test View getting retained w/ RETAIN_DETACH
        controller.retainView = true
        view = controller.inflate(router.container!!)
        assertNotNull(controller.view)
        ViewUtils.reportAttached(view, true)
        assertNotNull(controller.view)
        ViewUtils.reportAttached(view, false)
        assertNotNull(controller.view)

        // Ensure re-setting RELEASE_DETACH releases
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

        val parentView = parent.view
        val child1View = child1.view
        val child2View = child2.view
        val child3View = child3.view

        val onTopController = TestController()

        router.pushController(onTopController.toTransaction())

        assertNull(parent.view)
        assertNotNull(child1.view)
        assertNull(child2.view)
        assertNotNull(child3.view)

        router.popCurrentController()

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

        router.setRoot(parent.toTransaction())

        parent.getChildRouter(parent.childContainer1!!)
            .setRoot(child1.toTransaction())

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