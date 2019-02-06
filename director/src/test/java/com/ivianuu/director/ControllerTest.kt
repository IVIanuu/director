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
import com.ivianuu.director.util.reportAttached
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.attachRouter(activityProxy.view).apply {
        if (!hasRootController) {
            setRoot(TestController().toTransaction())
        }
    }

    @Test
    fun testAddRemoveChildControllers() {
        val parent = TestController()
        val child1 = TestController()
        val child2 = TestController()

        router.pushController(parent.toTransaction())

        assertEquals(0, parent.childRouters.size)

        var childRouter =
            parent.getChildRouter(parent.childContainer1!!)
                .popsLastView(true)

        childRouter.setRoot(child1.toTransaction())

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(1, childRouter.backstack.size)
        assertEquals(child1, childRouter.backstack.firstOrNull()?.controller)
        assertEquals(parent, child1.parentController)

        childRouter =
                parent.getChildRouter(parent.childContainer1!!)
        childRouter.pushController(child2.toTransaction())

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(2, childRouter.backstack.size)
        assertEquals(child1, childRouter.backstack.firstOrNull()?.controller)
        assertEquals(child2, childRouter.backstack[1].controller)
        assertEquals(parent, child1.parentController)
        assertEquals(parent, child2.parentController)

        childRouter.popController(child2)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(1, childRouter.backstack.size)
        assertEquals(child1, childRouter.backstack.firstOrNull()?.controller)
        assertEquals(parent, child1.parentController)

        childRouter.popController(child1)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(0, childRouter.backstack.size)
    }

    @Test
    fun testAddRemoveChildRouters() {
        val parent = TestController()

        val child1 = TestController()
        val child2 = TestController()

        router.pushController(parent.toTransaction())

        assertEquals(0, parent.childRouters.size)

        val childRouter1 =
            parent.getChildRouter(parent.childContainer1!!)
        val childRouter2 =
            parent.getChildRouter(parent.childContainer2!!)

        childRouter1.setRoot(child1.toTransaction())
        childRouter2.setRoot(child2.toTransaction())

        assertEquals(2, parent.childRouters.size)
        assertEquals(childRouter1, parent.childRouters.firstOrNull())
        assertEquals(childRouter2, parent.childRouters[1])
        assertEquals(1, childRouter1.backstack.size)
        assertEquals(1, childRouter2.backstack.size)
        assertEquals(child1, childRouter1.backstack.firstOrNull()?.controller)
        assertEquals(child2, childRouter2.backstack.firstOrNull()?.controller)
        assertEquals(parent, child1.parentController)
        assertEquals(parent, child2.parentController)

        parent.removeChildRouter(childRouter2)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter1, parent.childRouters.firstOrNull())
        assertEquals(1, childRouter1.backstack.size)
        assertEquals(0, childRouter2.backstack.size)
        assertEquals(child1, childRouter1.backstack.firstOrNull()?.controller)
        assertEquals(parent, child1.parentController)

        parent.removeChildRouter(childRouter1)

        assertEquals(0, parent.childRouters.size)
        assertEquals(0, childRouter1.backstack.size)
        assertEquals(0, childRouter2.backstack.size)
    }

    @Test
    fun testRestoredChildRouterBackstack() {
        val parent = TestController()
        router.pushController(parent.toTransaction())
        parent.view!!.reportAttached(true)

        val childTransaction1 = TestController().toTransaction()
        val childTransaction2 = TestController().toTransaction()

        var childRouter =
            parent.getChildRouter(parent.childContainer1!!)
                .popsLastView(true)

        childRouter.setRoot(childTransaction1)
        childRouter.pushController(childTransaction2)

        val savedState = childRouter.saveInstanceState()
        parent.removeChildRouter(childRouter)

        childRouter =
                parent.getChildRouter(parent.childContainer2!!)
        assertEquals(0, childRouter.backstack.size)

        childRouter.restoreInstanceState(savedState)
        childRouter.rebind()

        assertEquals(2, childRouter.backstack.size)

        val restoredChildTransaction1 = childRouter.backstack.first()
        val restoredChildTransaction2 = childRouter.backstack[1]

        assertEquals(childTransaction1.transactionIndex, restoredChildTransaction1.transactionIndex)
        assertEquals(
            childTransaction1.controller.instanceId,
            restoredChildTransaction1.controller.instanceId
        )
        assertEquals(childTransaction2.transactionIndex, restoredChildTransaction2.transactionIndex)
        assertEquals(
            childTransaction2.controller.instanceId,
            restoredChildTransaction2.controller.instanceId
        )

        assertTrue(parent.handleBack())
        assertEquals(1, childRouter.backstack.size)
        assertEquals(restoredChildTransaction1, childRouter.backstack[0])

        assertTrue(parent.handleBack())
        assertEquals(0, childRouter.backstack.size)
    }

}