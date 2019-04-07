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
import com.ivianuu.director.util.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RouterTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1)

    @Test
    fun testSetRoot() {
        val rootController = TestController()

        assertFalse(router.hasRoot)

        router.setRoot(rootController)

        assertTrue(router.hasRoot)

        assertEquals(rootController, router.backstack.firstOrNull())
    }

    @Test
    fun testPushPopControllers() {
        val controller1 = TestController()
        val controller2 = TestController()

        router.push(controller1)

        assertEquals(1, router.backstackSize)
        assertEquals(controller1, router.backstack[0])

        router.push(controller2)

        assertEquals(2, router.backstackSize)
        assertEquals(controller1, router.backstack[0])
        assertEquals(controller2, router.backstack[1])

        router.popTop()

        assertEquals(1, router.backstackSize)
        assertEquals(controller1, router.backstack[0])

        router.popTop()

        assertEquals(0, router.backstackSize)
    }

    @Test
    fun testPopToTag() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"
        val controller3Tag = "controller3"
        val controller4Tag = "controller4"

        val controller1 = TestController()
        val controller2 = TestController()
        val controller3 = TestController()
        val controller4 = TestController()

        router.push(controller1.tag(controller1Tag))
        router.push(controller2.tag(controller2Tag))
        router.push(controller3.tag(controller3Tag))
        router.push(controller4.tag(controller4Tag))

        router.popToTag(controller2Tag)

        assertEquals(2, router.backstackSize)
        assertEquals(controller1, router.getControllerByTagOrNull(controller1Tag))
        assertEquals(controller2, router.getControllerByTagOrNull(controller2Tag))
        assertNull(router.getControllerByTagOrNull(controller3Tag))
        assertNull(router.getControllerByTagOrNull(controller4Tag))
    }

    @Test
    fun testPopNonCurrent() {
        val controller1 = TestController()
        val controller2 = TestController()
        val controller3 = TestController()

        router.push(controller1)
        router.push(controller2)
        router.push(controller3)

        router.pop(controller2)

        assertEquals(2, router.backstackSize)
        assertEquals(controller1, router.backstack[0])
        assertFalse(router.backstack.contains(controller2))
        assertEquals(controller3, router.backstack[1])
    }

    @Test
    fun testSetBackstack() {
        router.setRoot(TestController())

        assertEquals(1, router.backstackSize)

        val rootController = TestController()
        val middleController = TestController()
        val topController = TestController()

        val backstack =
            listOf(rootController, middleController, topController)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        val fetchedBackstack = router.backstack
        assertEquals(rootController, fetchedBackstack[0])
        assertEquals(middleController, fetchedBackstack[1])
        assertEquals(topController, fetchedBackstack[2])

        assertEquals(router, rootController.router)
        assertEquals(router, middleController.router)
        assertEquals(router, topController.router)
    }

    @Test
    fun testSetBackstackWithNoRemoveViewOnPush() {
        val oldRootController = TestController()
        val oldTopController = TestController()
            .pushChangeHandler(noRemoveViewOnPushHandler())

        router.setRoot(oldRootController)
        router.push(oldTopController)
        assertEquals(2, router.backstackSize)

        assertTrue(oldRootController.isAttached)
        assertTrue(oldTopController.isAttached)

        val rootController = TestController()
        val middleController = TestController()
            .pushChangeHandler(noRemoveViewOnPushHandler())
        val topController = TestController()
            .pushChangeHandler(noRemoveViewOnPushHandler())

        val backstack = listOf(rootController, middleController, topController)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        val fetchedBackstack = router.backstack
        assertEquals(rootController, fetchedBackstack[0])
        assertEquals(middleController, fetchedBackstack[1])
        assertEquals(topController, fetchedBackstack[2])

        assertFalse(oldRootController.isAttached)
        assertFalse(oldTopController.isAttached)
        assertTrue(rootController.isAttached)
        assertTrue(middleController.isAttached)
        assertTrue(topController.isAttached)
    }

    @Test
    fun testPopToRoot() {
        val rootController = TestController()
        val controller1 = TestController()
        val controller2 = TestController()

        val backstack =
            listOf(rootController, controller1, controller2)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        router.popToRoot()

        assertEquals(1, router.backstackSize)
        assertEquals(rootController, router.backstack[0])

        assertTrue(rootController.isAttached)
        assertFalse(controller1.isAttached)
        assertFalse(controller2.isAttached)
    }

    @Test
    fun testPopToRootWithNoRemoveViewOnPush() {
        val rootController = TestController()
            .pushChangeHandler(DefaultChangeHandler(false))
        val controller1 = TestController()
            .pushChangeHandler(DefaultChangeHandler(false))
        val controller2 = TestController()
            .pushChangeHandler(DefaultChangeHandler(false))

        val backstack =
            listOf(rootController, controller1, controller2)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        router.popToRoot()

        assertEquals(1, router.backstackSize)
        assertEquals(rootController, router.backstack[0])

        assertTrue(rootController.isAttached)
        assertFalse(controller1.isAttached)
        assertFalse(controller2.isAttached)
    }

    @Test
    fun testReplaceTopController() {
        val rootController = TestController()
        val topController = TestController()

        val backstack = listOf(rootController, topController)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        var fetchedBackstack = router.backstack
        assertEquals(rootController, fetchedBackstack[0])
        assertEquals(topController, fetchedBackstack[1])

        val newTopController = TestController()
        router.replaceTop(newTopController)

        assertEquals(2, router.backstackSize)

        fetchedBackstack = router.backstack
        assertEquals(rootController, fetchedBackstack[0])
        assertEquals(newTopController, fetchedBackstack[1])
    }

    @Test
    fun testReplaceTopControllerWithNoRemoveViewOnPush() {
        val controllerA = TestController()
        val controllerB = TestController()
            .changeHandler(noRemoveViewOnPushHandler())

        val backstack = listOf(controllerA, controllerB)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.isAttached)
        assertTrue(controllerB.isAttached)

        val controllerC = TestController()
            .changeHandler(noRemoveViewOnPushHandler())
        router.replaceTop(controllerC)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.isAttached)
        assertFalse(controllerB.isAttached)
        assertTrue(controllerC.isAttached)
    }

    @Test
    fun testReplaceTopControllerWithMixedRemoveViewOnPush1() {
        val controllerA = TestController()
        val controllerB = TestController()
            .changeHandler(noRemoveViewOnPushHandler())

        val backstack = listOf(controllerA, controllerB)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.isAttached)
        assertTrue(controllerB.isAttached)

        val controllerC = TestController()
        router.replaceTop(controllerC)

        assertEquals(2, router.backstackSize)

        assertFalse(controllerA.isAttached)
        assertFalse(controllerB.isAttached)
        assertTrue(controllerC.isAttached)
    }

    @Test
    fun testReplaceTopControllerWithMixedRemoveViewOnPush2() {
        val controllerA = TestController()
        val controllerB = TestController()

        val backstack = listOf(controllerA, controllerB)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        assertFalse(controllerA.isAttached)
        assertTrue(controllerB.isAttached)

        val controllerC = TestController()
            .changeHandler(noRemoveViewOnPushHandler())
        router.replaceTop(controllerC)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.isAttached)
        assertFalse(controllerB.isAttached)
        assertTrue(controllerC.isAttached)
    }

    @Test
    fun testAddBetweenWithNoRemoveView() {
        // todo implement
    }

    @Test
    fun testSettingSameBackstackNoOps() {
        var changeCalls = 0
        router.doOnChangeStarted { _, _, _, _, _, _ -> changeCalls++ }

        val backstack = listOf(
            TestController(),
            TestController(),
            TestController()
        )

        router.setBackstack(backstack, true)
        assertEquals(1, changeCalls)

        router.setBackstack(backstack, true)
        assertEquals(1, changeCalls)
    }

    @Test
    fun testSettingSameVisibleControllersNoOps() {
        var changeCalls = 0
        router.doOnChangeStarted { _, _, _, _, _, _ -> changeCalls++ }

        val backstack = listOf(
            TestController(),
            TestController(),
            TestController()
                .changeHandler(noRemoveViewOnPushHandler())
        )

        router.setBackstack(backstack, true)
        assertEquals(2, changeCalls)

        router.setBackstack(backstack.toMutableList().apply { removeAt(0) }, true)
        assertEquals(2, changeCalls)
    }

    @Test
    fun testRearrangeBackstack() {
        router.popsLastView = true
        val controller1 = TestController()
        val controller2 = TestController()

        var backstack = listOf(controller1, controller2)
        router.setBackstack(backstack, true)

        assertEquals(1, controller1.transactionIndex)
        assertEquals(2, controller2.transactionIndex)

        backstack = listOf(controller2, controller1)
        router.setBackstack(backstack, true)

        assertEquals(1, controller2.transactionIndex)
        assertEquals(2, controller1.transactionIndex)

        router.handleBack()

        assertEquals(1, router.backstackSize)
        assertEquals(controller2, router.backstack[0])

        router.handleBack()
        assertEquals(0, router.backstackSize)
    }

    @Test
    fun testGetByInstanceId() {
        val controller = TestController()

        router.push(controller)

        assertEquals(controller, router.getControllerByInstanceIdOrNull(controller.instanceId))
        assertNull(router.getControllerByInstanceIdOrNull("fake id"))
    }

    @Test
    fun testGetByTag() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"

        val controller1 = TestController()
        val controller2 = TestController()

        router.push(controller1.tag(controller1Tag))
        router.push(controller2.tag(controller2Tag))

        assertEquals(controller1, router.getControllerByTagOrNull(controller1Tag))
        assertEquals(controller2, router.getControllerByTagOrNull(controller2Tag))
    }

    @Test
    fun testRecursivelySettingRouterListener() {
        val routerRecursiveListener = EmptyChangeListener()
        val routerNonRecursiveListener = EmptyChangeListener()

        val controller1 = TestController()
        router.addListener(routerRecursiveListener, true)
        router.addListener(routerNonRecursiveListener)
        router.setRoot(controller1)

        val childRouterRecursiveListener = EmptyChangeListener()
        val childRouterNonRecursiveListener = EmptyChangeListener()

        val childRouter =
            controller1.getChildRouter(controller1.childContainer1!!)
        assertTrue(childRouter.getListeners(false).contains(routerRecursiveListener))
        assertFalse(childRouter.getListeners(false).contains(routerNonRecursiveListener))

        val controller2 = TestController()
        childRouter.addListener(childRouterRecursiveListener, true)
        childRouter.addListener(childRouterNonRecursiveListener)
        childRouter.setRoot(controller2)

        val childRouter2 =
            controller2.getChildRouter(controller2.childContainer2!!)
        val controller3 = TestController()
        childRouter2.push(controller3)
        assertTrue(childRouter2.getListeners(false).contains(routerRecursiveListener))
        assertTrue(childRouter2.getListeners(false).contains(childRouterRecursiveListener))
        assertFalse(
            childRouter2.getListeners(false).contains(
                childRouterNonRecursiveListener
            )
        )
    }

    @Test
    fun testRecursivelySettingLifecycleListener() {
        val routerRecursiveListener = EmptyControllerListener()
        val routerNonRecursiveListener = EmptyControllerListener()

        val controller1 = TestController()
        router.addControllerListener(routerRecursiveListener, true)
        router.addControllerListener(routerNonRecursiveListener)
        router.setRoot(controller1)

        val childRouterRecursiveListener = EmptyControllerListener()
        val childRouterNonRecursiveListener = EmptyControllerListener()

        val childRouter =
            controller1.getChildRouter(controller1.childContainer1!!)
        assertTrue(childRouter.getControllerListeners(false).contains(routerRecursiveListener))
        assertFalse(childRouter.getControllerListeners(false).contains(routerNonRecursiveListener))

        val controller2 = TestController()
        childRouter.addControllerListener(childRouterRecursiveListener, true)
        childRouter.addControllerListener(childRouterNonRecursiveListener)
        childRouter.setRoot(controller2)

        val childRouter2 =
            controller2.getChildRouter(controller2.childContainer2!!)
        val controller3 = TestController()
        childRouter2.push(controller3)
        assertTrue(childRouter2.getControllerListeners(false).contains(routerRecursiveListener))
        assertTrue(
            childRouter2.getControllerListeners(false).contains(
                childRouterRecursiveListener
            )
        )
        assertFalse(
            childRouter2.getControllerListeners(false).contains(
                childRouterNonRecursiveListener
            )
        )
    }
}