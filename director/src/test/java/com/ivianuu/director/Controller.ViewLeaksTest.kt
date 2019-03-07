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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ViewLeakTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1).apply {
        if (!hasRoot) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testPop() {
        val controller = TestController()
        router.push(controller.toTransaction())

        assertNotNull(controller.view)

        router.popTop()

        assertNull(controller.view)
    }

    @Test
    fun testPopWhenPushNeverAdded() {
        val controller = TestController()
        router.push(
            controller.toTransaction()
                .pushChangeHandler(NeverAddChangeHandler())
        )

        assertNotNull(controller.view)

        router.popTop()

        assertNull(controller.view)
    }

    @Test
    fun testPopWhenPushNeverCompleted() {
        val controller = TestController()
        router.push(
            controller.toTransaction()
                .pushChangeHandler(NeverCompleteChangeHandler())
        )

        assertNotNull(controller.view)

        router.popTop()

        assertNull(controller.view)
    }

    @Test
    fun testActivityDestroyWhenPushNeverAdded() {
        val controller = TestController()
        router.push(
            controller.toTransaction()
                .pushChangeHandler(NeverAddChangeHandler())
        )

        assertNotNull(controller.view)

        activityProxy.pause().stop().destroy()

        assertNull(controller.view)
    }

    class NeverAddChangeHandler : ChangeHandler() {
        override fun performChange(changeData: ChangeData) {
            changeData.from?.let(changeData.container::removeView)
        }
    }

    class NeverCompleteChangeHandler : ChangeHandler() {
        override fun performChange(changeData: ChangeData) {
            changeData.from?.let(changeData.container::removeView)
            changeData.container.addView(changeData.to, changeData.toIndex)
        }
    }
}