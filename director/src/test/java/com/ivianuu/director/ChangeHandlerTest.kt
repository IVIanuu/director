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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ChangeHandlerTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1)

    @Test
    fun testSaveRestore() {
        val changeHandler1 = DefaultChangeHandler(false)
        val changeHandler2 = DefaultChangeHandler(true)

        val controller = TestController()
            .pushChangeHandler(changeHandler1).popChangeHandler(changeHandler2)

        router.push(controller)

        router.restoreInstanceState(router.saveInstanceState())

        val restoredController = router.backstack.first()

        val restored1 = restoredController.pushChangeHandler
        val restored2 = restoredController.popChangeHandler

        assertEquals(changeHandler1.javaClass, restored1?.javaClass)
        assertEquals(changeHandler2.javaClass, restored2?.javaClass)

        val restored1Cast = restored1 as DefaultChangeHandler
        val restored2Cast = restored2 as DefaultChangeHandler

        assertEquals(
            changeHandler1.removesFromViewOnPush,
            restored1Cast.removesFromViewOnPush
        )

        assertEquals(
            changeHandler2.removesFromViewOnPush,
            restored2Cast.removesFromViewOnPush
        )
    }
}