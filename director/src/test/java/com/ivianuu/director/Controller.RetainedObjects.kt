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
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerRetainedObjectsTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = attachRouter(activityProxy.activity, activityProxy.view).apply {
        if (!hasRootController) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testObjectsRetainedOnConfigurationChange() {
        var controller = router.backstack.first().controller

        controller.retainedObjects.put("key", "value")

        activityProxy.rotate()

        controller = router.backstack.first().controller

        assertEquals(controller.retainedObjects["key"], "value")
    }

    @Test
    fun testObjectsRemovedOnActivityDestroy() {
        val controller = router.backstack.first().controller

        controller.retainedObjects.put("key", "value")

        activityProxy.pause().stop(false).destroy()

        assertNull(controller.retainedObjects["key"])
    }

    @Test
    fun testObjectsRemovedOnDestroy() {
        val controller = router.backstack.first().controller

        controller.retainedObjects.put("key", "value")

        router.popController(controller)

        assertNull(controller.retainedObjects["key"])
    }
}