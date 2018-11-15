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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.CallState
import com.ivianuu.director.util.TestController
import com.ivianuu.director.util.ViewUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// todo test child controller view retained with parent NOT retained

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = attachRouter(activityProxy.activity, activityProxy.view).apply {
        if (!hasRootController) {
            setRoot(TestController().toTransaction())
        }
    }

    // todo move to router?
    @Test
    fun testActivityResult() {
        val controller = TestController()
        val expectedCallState = CallState().setupForAddedControllers()

        router.pushController(controller.toTransaction())

        // Ensure that calling onActivityResult w/o requesting a result doesn't do anything
        router.onActivityResult(setOf(), 1, Activity.RESULT_OK, null)
        assertCalls(expectedCallState, controller)

        // Ensure starting an activity for result gets us the result back
        controller.startActivityForResult(Intent("action"), 1)
        router.onActivityResult(setOf(controller.instanceId), 1, Activity.RESULT_OK, null)
        expectedCallState.onActivityResultCalls++
        assertCalls(expectedCallState, controller)

        // Ensure requesting a result w/o calling startActivityForResult works
        controller.registerForActivityResult(2)
        router.onActivityResult(setOf(controller.instanceId), 2, Activity.RESULT_OK, null)
        expectedCallState.onActivityResultCalls++
        assertCalls(expectedCallState, controller)
    }

    // todo move to router?
    @Test
    fun testActivityResultForChild() {
        val parent = TestController()
        val child = TestController()

        router.pushController(parent.toTransaction())
        parent.getChildRouter(parent.childContainer1!!)
            .setRoot(child.toTransaction())

        val childExpectedCallState = CallState().setupForAddedControllers()
        val parentExpectedCallState = CallState().setupForAddedControllers()

        // Ensure that calling onActivityResult w/o requesting a result doesn't do anything
        router.onActivityResult(setOf(), 1, Activity.RESULT_OK, null)
        assertCalls(childExpectedCallState, child)
        assertCalls(parentExpectedCallState, parent)

        // Ensure starting an activity for result gets us the result back
        child.startActivityForResult(Intent("action"), 1)
        router.onActivityResult(setOf(child.instanceId), 1, Activity.RESULT_OK, null)
        childExpectedCallState.onActivityResultCalls++
        assertCalls(childExpectedCallState, child)
        assertCalls(parentExpectedCallState, parent)

        // Ensure requesting a result w/o calling startActivityForResult works
        child.registerForActivityResult(2)
        router.onActivityResult(setOf(child.instanceId), 2, Activity.RESULT_OK, null)
        childExpectedCallState.onActivityResultCalls++
        assertCalls(childExpectedCallState, child)
        assertCalls(parentExpectedCallState, parent)
    }

    @Test
    fun testPermissionResult() {
        val requestedPermissions = arrayOf("test")

        val controller = TestController()
        val expectedCallState = CallState().setupForAddedControllers()

        router.pushController(controller.toTransaction())

        // Ensure that calling handleRequestedPermission w/o requesting a result doesn't do anything
        router.onRequestPermissionsResult(
            setOf("anotherId"),
            1,
            requestedPermissions,
            intArrayOf(1)
        )
        assertCalls(expectedCallState, controller)

        // Ensure requesting the permission gets us the result back
        try {
            controller.requestPermissions(requestedPermissions, 1)
        } catch (ignored: NoSuchMethodError) {
        }

        router.onRequestPermissionsResult(
            setOf(controller.instanceId),
            1,
            requestedPermissions,
            intArrayOf(1)
        )
        expectedCallState.onRequestPermissionsResultCalls++
        assertCalls(expectedCallState, controller)
    }

    @Test
    fun testPermissionResultForChild() {
        val requestedPermissions = arrayOf("test")

        val parent = TestController()
        val child = TestController()

        router.pushController(parent.toTransaction())
        parent.getChildRouter(parent.childContainer1!!)
            .setRoot(child.toTransaction())

        val childExpectedCallState = CallState().setupForAddedControllers()
        val parentExpectedCallState = CallState().setupForAddedControllers()

        // Ensure that calling handleRequestedPermission w/o requesting a result doesn't do anything
        router.onRequestPermissionsResult(
            setOf("anotherId"),
            1,
            requestedPermissions,
            intArrayOf(1)
        )
        assertCalls(childExpectedCallState, child)
        assertCalls(parentExpectedCallState, parent)

        // Ensure requesting the permission gets us the result back
        try {
            child.requestPermissions(requestedPermissions, 1)
        } catch (ignored: NoSuchMethodError) {
        }

        router.onRequestPermissionsResult(
            setOf(child.instanceId),
            1,
            requestedPermissions,
            intArrayOf(1)
        )
        childExpectedCallState.onRequestPermissionsResultCalls++
        assertCalls(childExpectedCallState, child)
        assertCalls(parentExpectedCallState, parent)
    }

    @Test
    fun testAddRemoveChildControllers() {
        val parent = TestController()
        val child1 = TestController()
        val child2 = TestController()

        router.pushController(parent.toTransaction())

        assertEquals(0, parent.childRouters.size)
        assertNull(child1.parentController)
        assertNull(child2.parentController)

        var childRouter =
            parent.getChildRouter(parent.childContainer1!!)
        childRouter.popsLastView = true
        childRouter.setRoot(child1.toTransaction())

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(1, childRouter.backstack.size)
        assertEquals(child1, childRouter.backstack.firstOrNull()?.controller)
        assertEquals(parent, child1.parentController)
        assertNull(child2.parentController)

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
        assertNull(child2.parentController)

        childRouter.popController(child1)

        assertEquals(1, parent.childRouters.size)
        assertEquals(childRouter, parent.childRouters.firstOrNull())
        assertEquals(0, childRouter.backstack.size)
        assertNull(child1.parentController)
        assertNull(child2.parentController)
    }

    @Test
    fun testAddRemoveChildRouters() {
        val parent = TestController()

        val child1 = TestController()
        val child2 = TestController()

        router.pushController(parent.toTransaction())

        assertEquals(0, parent.childRouters.size)
        assertNull(child1.parentController)
        assertNull(child2.parentController)

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
        assertNull(child2.parentController)

        parent.removeChildRouter(childRouter1)

        assertEquals(0, parent.childRouters.size)
        assertEquals(0, childRouter1.backstack.size)
        assertEquals(0, childRouter2.backstack.size)
        assertNull(child1.parentController)
        assertNull(child2.parentController)
    }

    @Test
    fun testRestoredChildRouterBackstack() {
        val parent = TestController()
        router.pushController(parent.toTransaction())
        ViewUtils.reportAttached(parent.view!!, true)

        val childTransaction1 = TestController().toTransaction()
        val childTransaction2 = TestController().toTransaction()

        var childRouter =
            parent.getChildRouter(parent.childContainer1!!)
        childRouter.popsLastView = true
        childRouter.setRoot(childTransaction1)
        childRouter.pushController(childTransaction2)

        val savedState = Bundle()
        childRouter.saveInstanceState(savedState)
        parent.removeChildRouter(childRouter)

        childRouter =
                parent.getChildRouter(parent.childContainer2!!)
        assertEquals(0, childRouter.backstack.size)

        childRouter.restoreInstanceState(savedState)
        childRouter.rebindIfNeeded()

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

    private fun assertCalls(callState: CallState, controller: TestController) {
        assertEquals(
            "Expected call counts and controller call counts do not match.",
            callState,
            controller.currentCallState
        )
    }
}