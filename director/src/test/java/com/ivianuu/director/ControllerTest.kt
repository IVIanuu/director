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
import com.ivianuu.director.ControllerState.VIEW_BOUND
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
    private val router = activityProxy.activity.getOrCreateRouter(activityProxy.view).apply {
        setRootIfEmpty { controller(TestController()) }
    }

    @Test
    fun testAddRemoveChildControllers() {
        val parent = TestController()
        val child1 = TestController()
        val child2 = TestController()

        router.push(parent)

        assertEquals(0, parent.childRouters.size)

        var childRouter =
            parent.getChildRouter(parent.childContainer1!!)
                .popsLastView(true)

        childRouter.setRoot(child1)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(1, childRouter.backstack.size)
        assertEquals(child1, childRouter.backstack.firstOrNull()?.controller)
        assertEquals(parent, child1.parentController)

        childRouter =
                parent.getChildRouter(parent.childContainer1!!)
        childRouter.push(child2)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(2, childRouter.backstack.size)
        assertEquals(child1, childRouter.backstack.firstOrNull()?.controller)
        assertEquals(child2, childRouter.backstack[1].controller)
        assertEquals(parent, child1.parentController)
        assertEquals(parent, child2.parentController)

        childRouter.pop(child2)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(1, childRouter.backstack.size)
        assertEquals(child1, childRouter.backstack.firstOrNull()?.controller)
        assertEquals(parent, child1.parentController)

        childRouter.pop(child1)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(0, childRouter.backstack.size)
    }

    @Test
    fun testAddRemoveChildRouters() {
        val parent = TestController()

        val child1 = TestController()
        val child2 = TestController()

        router.push(parent)

        assertEquals(0, parent.childRouters.size)

        val childRouter1 =
            parent.getChildRouter(parent.childContainer1!!)
        val childRouter2 =
            parent.getChildRouter(parent.childContainer2!!)

        childRouter1.setRoot(child1)
        childRouter2.setRoot(child2)

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
        router.push(parent)

        val childTransaction1 = TestController().toTransaction()
        val childTransaction2 = TestController().toTransaction()

        var childRouter =
            parent.getChildRouter(parent.childContainer1!!)
                .popsLastView(true)

        childRouter.setRoot(childTransaction1)
        childRouter.push(childTransaction2)

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

    @Test
    fun testAttachedToUnownedParent() {
        val controller = TestController()

        controller.doOnPostBindView { _, view, _ ->
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
        router.hostStarted()
        assertTrue(controller.isAttached)
    }

    @Test
    fun testControllerState() {
        val controller = TestController()

        assertEquals(INITIALIZED, controller.state)

        controller.addLifecycleListener {
            preCreate { _, _ -> assertEquals(INITIALIZED, controller.state) }
            postCreate { _, _ -> assertEquals(CREATED, controller.state) }
            preBindView { _, _, _ -> assertEquals(CREATED, controller.state) }
            postBindView { _, _, _ -> assertEquals(VIEW_BOUND, controller.state) }
            preAttach { _, _ -> assertEquals(VIEW_BOUND, controller.state) }
            postAttach { _, _ -> assertEquals(ATTACHED, controller.state) }
            preDetach { _, _ -> assertEquals(ATTACHED, controller.state) }
            postDetach { _, _ -> assertEquals(VIEW_BOUND, controller.state) }
            preUnbindView { _, _ -> assertEquals(VIEW_BOUND, controller.state) }
            postUnbindView { assertEquals(CREATED, controller.state) }
            preDestroy { assertEquals(CREATED, controller.state) }
            postDestroy { assertEquals(DESTROYED, controller.state) }
        }

        router.push(controller)
        controller.doOnPostAttach { _, _ -> router.popCurrent() }
    }
}