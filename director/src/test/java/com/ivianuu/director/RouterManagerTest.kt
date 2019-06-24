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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RouterManagerTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val manager = RouterManager()

    @Test
    fun testAddRemoveRouters() {
        assertEquals(0, manager.routers.size)

        val router1 =
            manager.getRouter(activityProxy.view1)
        val router2 =
            manager.getRouter(activityProxy.view2)

        assertEquals(2, manager.routers.size)
        assertEquals(router1, manager.routers[0])
        assertEquals(router2, manager.routers[1])

        manager.removeRouter(router2)

        assertEquals(1, manager.routers.size)
        assertEquals(router1, manager.routers[0])

        manager.removeRouter(router1)

        assertEquals(0, manager.routers.size)
    }

}