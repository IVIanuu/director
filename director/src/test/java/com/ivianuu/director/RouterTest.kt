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
import com.ivianuu.director.util.EmptyChangeListener
import com.ivianuu.director.util.EmptyControllerListener
import com.ivianuu.director.util.TestController
import com.ivianuu.director.util.noRemoveViewOnPushHandler
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
        val rootTag = "root"

        val rootController = TestController()

        assertFalse(router.hasRoot)

        router.setRoot(rootController.toTransaction().tag(rootTag))

        assertTrue(router.hasRoot)

        assertEquals(rootController, router.getControllerByTagOrNull(rootTag))
    }

    @Test
    fun testSetNewRoot() {
        val oldRootTag = "oldRoot"
        val newRootTag = "newRoot"

        val oldRootController = TestController()
        val newRootController = TestController()

        router.setRoot(oldRootController.toTransaction().tag(oldRootTag))
        router.setRoot(newRootController.toTransaction().tag(newRootTag))

        assertNull(router.getControllerByTagOrNull(oldRootTag))
        assertEquals(newRootController, router.getControllerByTagOrNull(newRootTag))
    }

    @Test
    fun testGetByInstanceId() {
        val controller = TestController()

        router.push(controller.toTransaction())

        assertEquals(controller, router.getControllerByInstanceIdOrNull(controller.instanceId))
        assertNull(router.getControllerByInstanceIdOrNull("fake id"))
    }

    @Test
    fun testGetByTag() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"

        val controller1 = TestController()
        val controller2 = TestController()

        router.push(controller1.toTransaction().tag(controller1Tag))
        router.push(controller2.toTransaction().tag(controller2Tag))

        assertEquals(controller1, router.getControllerByTagOrNull(controller1Tag))
        assertEquals(controller2, router.getControllerByTagOrNull(controller2Tag))
    }

    @Test
    fun testPushPopControllers() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"

        val controller1 = TestController()
        val controller2 = TestController()

        router.push(controller1.toTransaction().tag(controller1Tag))

        assertEquals(1, router.backstackSize)

        router.push(controller2.toTransaction().tag(controller2Tag))

        assertEquals(2, router.backstackSize)

        router.popTop()

        assertEquals(1, router.backstackSize)

        assertEquals(controller1, router.getControllerByTagOrNull(controller1Tag))
        assertNull(router.getControllerByTagOrNull(controller2Tag))

        router.popTop()

        assertEquals(0, router.backstackSize)

        assertNull(router.getControllerByTagOrNull(controller1Tag))
        assertNull(router.getControllerByTagOrNull(controller2Tag))
    }

    @Test
    fun testPopControllerConcurrentModificationException() {
        var step = 1
        var i = 0
        while (i < 10) {
            router.push(TestController().toTransaction().tag("1"))
            router.push(TestController().toTransaction().tag("1"))
            router.push(TestController().toTransaction().tag("1"))

            val tag: String
            when (step) {
                1 -> tag = "1"
                2 -> tag = "2"
                else -> {
                    tag = "3"
                    step = 0
                }
            }
            val controller = router.getControllerByTagOrNull(tag)
            if (controller != null) {
                router.pop(controller)
            }
            router.popToRoot()
            i++
            step++
        }
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

        router.push(controller1.toTransaction().tag(controller1Tag))
        router.push(controller2.toTransaction().tag(controller2Tag))
        router.push(controller3.toTransaction().tag(controller3Tag))
        router.push(controller4.toTransaction().tag(controller4Tag))

        router.popToTag(controller2Tag)

        assertEquals(2, router.backstackSize)
        assertEquals(controller1, router.getControllerByTagOrNull(controller1Tag))
        assertEquals(controller2, router.getControllerByTagOrNull(controller2Tag))
        assertNull(router.getControllerByTagOrNull(controller3Tag))
        assertNull(router.getControllerByTagOrNull(controller4Tag))
    }

    @Test
    fun testPopNonCurrent() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"
        val controller3Tag = "controller3"

        val controller1 = TestController()
        val controller2 = TestController()
        val controller3 = TestController()

        router.push(controller1.toTransaction().tag(controller1Tag))
        router.push(controller2.toTransaction().tag(controller2Tag))
        router.push(controller3.toTransaction().tag(controller3Tag))

        router.pop(controller2)

        assertEquals(2, router.backstackSize)
        assertEquals(controller1, router.getControllerByTagOrNull(controller1Tag))
        assertNull(router.getControllerByTagOrNull(controller2Tag))
        assertEquals(controller3, router.getControllerByTagOrNull(controller3Tag))
    }

    @Test
    fun testSetBackstack() {
        router.setRoot(TestController().toTransaction())

        assertEquals(1, router.backstackSize)

        val rootTransaction = TestController().toTransaction()
        val middleTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()

        val backstack =
            listOf(rootTransaction, middleTransaction, topTransaction)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        val fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(middleTransaction, fetchedBackstack[1])
        assertEquals(topTransaction, fetchedBackstack[2])

        assertEquals(router, rootTransaction.controller.router)
        assertEquals(router, middleTransaction.controller.router)
        assertEquals(router, topTransaction.controller.router)
    }

    @Test
    fun testSetBackstackWithNoRemoveViewOnPush() {
        val oldRootTransaction = TestController().toTransaction()
        val oldTopTransaction = TestController().toTransaction()
            .pushChangeHandler(noRemoveViewOnPushHandler())

        router.setRoot(oldRootTransaction)
        router.push(oldTopTransaction)
        assertEquals(2, router.backstackSize)

        assertTrue(oldRootTransaction.controller.isAttached)
        assertTrue(oldTopTransaction.controller.isAttached)

        val rootTransaction = TestController().toTransaction()
        val middleTransaction = TestController().toTransaction()
            .pushChangeHandler(noRemoveViewOnPushHandler())
        val topTransaction = TestController().toTransaction()
            .pushChangeHandler(noRemoveViewOnPushHandler())

        val backstack = listOf(rootTransaction, middleTransaction, topTransaction)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        val fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(middleTransaction, fetchedBackstack[1])
        assertEquals(topTransaction, fetchedBackstack[2])

        assertFalse(oldRootTransaction.controller.isAttached)
        assertFalse(oldTopTransaction.controller.isAttached)
        assertTrue(rootTransaction.controller.isAttached)
        assertTrue(middleTransaction.controller.isAttached)
        assertTrue(topTransaction.controller.isAttached)
    }

    @Test
    fun testPopToRoot() {
        val rootTransaction = TestController().toTransaction()
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        val backstack =
            listOf(rootTransaction, transaction1, transaction2)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        router.popToRoot()

        assertEquals(1, router.backstackSize)
        assertEquals(rootTransaction, router.backstack[0])

        assertTrue(rootTransaction.controller.isAttached)
        assertFalse(transaction1.controller.isAttached)
        assertFalse(transaction2.controller.isAttached)
    }

    @Test
    fun testPopToRootWithNoRemoveViewOnPush() {
        val rootTransaction = TestController().toTransaction()
            .pushChangeHandler(SimpleSwapChangeHandler(false))
        val transaction1 = TestController().toTransaction()
            .pushChangeHandler(SimpleSwapChangeHandler(false))
        val transaction2 = TestController().toTransaction()
            .pushChangeHandler(SimpleSwapChangeHandler(false))

        val backstack =
            listOf(rootTransaction, transaction1, transaction2)
        router.setBackstack(backstack, true)

        assertEquals(3, router.backstackSize)

        router.popToRoot()

        assertEquals(1, router.backstackSize)
        assertEquals(rootTransaction, router.backstack[0])

        assertTrue(rootTransaction.controller.isAttached)
        assertFalse(transaction1.controller.isAttached)
        assertFalse(transaction2.controller.isAttached)
    }

    @Test
    fun testReplaceTopController() {
        val rootTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()

        val backstack = listOf(rootTransaction, topTransaction)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        var fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(topTransaction, fetchedBackstack[1])

        val newTopTransaction = TestController().toTransaction()
        router.replaceTop(newTopTransaction)

        assertEquals(2, router.backstackSize)

        fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(newTopTransaction, fetchedBackstack[1])
    }

    @Test
    fun testReplaceTopControllerWithNoRemoveViewOnPush() {
        val controllerA = TestController().toTransaction()
        val controllerB = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())

        val backstack = listOf(controllerA, controllerB)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.controller.isAttached)
        assertTrue(controllerB.controller.isAttached)

        val controllerC = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())
        router.replaceTop(controllerC)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.controller.isAttached)
        assertFalse(controllerB.controller.isAttached)
        assertTrue(controllerC.controller.isAttached)
    }

    @Test
    fun testReplaceTopControllerWithMixedRemoveViewOnPush1() {
        val controllerA = TestController().toTransaction()
        val controllerB = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())

        val backstack = listOf(controllerA, controllerB)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.controller.isAttached)
        assertTrue(controllerB.controller.isAttached)

        val controllerC = TestController().toTransaction()
        router.replaceTop(controllerC)

        assertEquals(2, router.backstackSize)

        assertFalse(controllerA.controller.isAttached)
        assertFalse(controllerB.controller.isAttached)
        assertTrue(controllerC.controller.isAttached)
    }

    @Test
    fun testReplaceTopControllerWithMixedRemoveViewOnPush2() {
        val controllerA = TestController().toTransaction()
        val controllerB = TestController().toTransaction()

        val backstack = listOf(controllerA, controllerB)
        router.setBackstack(backstack, true)

        assertEquals(2, router.backstackSize)

        assertFalse(controllerA.controller.isAttached)
        assertTrue(controllerB.controller.isAttached)

        val controllerC = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())
        router.replaceTop(controllerC)

        assertEquals(2, router.backstackSize)

        assertTrue(controllerA.controller.isAttached)
        assertFalse(controllerB.controller.isAttached)
        assertTrue(controllerC.controller.isAttached)
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
            TestController().toTransaction(),
            TestController().toTransaction(),
            TestController().toTransaction()
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
            TestController().toTransaction(),
            TestController().toTransaction(),
            TestController().toTransaction()
                .changeHandler(noRemoveViewOnPushHandler())
        )

        router.setBackstack(backstack, true)
        assertEquals(2, changeCalls)

        router.setBackstack(backstack.toMutableList().apply { removeAt(0) }, true)
        assertEquals(2, changeCalls)
    }

    @Test
    fun testRearrangeTransactionBackstack() {
        router.popsLastView = true
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        var backstack = listOf(transaction1, transaction2)
        router.setBackstack(backstack, true)

        assertEquals(1, transaction1.transactionIndex)
        assertEquals(2, transaction2.transactionIndex)

        backstack = listOf(transaction2, transaction1)
        router.setBackstack(backstack, true)

        assertEquals(1, transaction2.transactionIndex)
        assertEquals(2, transaction1.transactionIndex)

        router.handleBack()

        assertEquals(1, router.backstackSize)
        assertEquals(transaction2, router.backstack[0])

        router.handleBack()
        assertEquals(0, router.backstackSize)
    }

    @Test
    fun testChildRouterRearrangeTransactionBackstack() {
        val parent = TestController()
        router.setRoot(parent.toTransaction())

        val childRouter =
            parent.getChildRouter(parent.childContainer1!!)

        childRouter.popsLastView = true

        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        var backstack = listOf(transaction1, transaction2)
        childRouter.setBackstack(backstack, true)

        assertEquals(2, transaction1.transactionIndex)
        assertEquals(3, transaction2.transactionIndex)

        backstack = listOf(transaction2, transaction1)
        childRouter.setBackstack(backstack, true)

        assertEquals(2, transaction2.transactionIndex)
        assertEquals(3, transaction1.transactionIndex)

        childRouter.handleBack()

        assertEquals(1, childRouter.backstackSize)
        assertEquals(transaction2, childRouter.backstack[0])

        childRouter.handleBack()
        assertEquals(0, childRouter.backstackSize)
    }

    @Test
    fun testIsBeingDestroyed() {
        val listener = ControllerListener(
            preUnbindView = { controller, _ -> assertTrue(controller.isBeingDestroyed) }
        )

        val controller1 = TestController()
        val controller2 = TestController()
        controller2.addListener(listener)

        router.setRoot(controller1.toTransaction())
        router.push(controller2.toTransaction())
        assertFalse(controller1.isBeingDestroyed)
        assertFalse(controller2.isBeingDestroyed)

        router.popTop()
        assertFalse(controller1.isBeingDestroyed)
        assertTrue(controller2.isBeingDestroyed)

        val controller3 = TestController()
        controller3.addListener(listener)
        router.push(controller3.toTransaction())
        assertFalse(controller1.isBeingDestroyed)
        assertFalse(controller3.isBeingDestroyed)

        router.popToRoot()
        assertFalse(controller1.isBeingDestroyed)
        assertTrue(controller3.isBeingDestroyed)
    }

    @Test
    fun testRecursivelySettingChangeListener() {
        val routerRecursiveListener = EmptyChangeListener()
        val routerNonRecursiveListener = EmptyChangeListener()

        val controller1 = TestController()
        router.addListener(routerRecursiveListener, true)
        router.addListener(routerNonRecursiveListener)
        router.setRoot(controller1.toTransaction())

        val childRouterRecursiveListener = EmptyChangeListener()
        val childRouterNonRecursiveListener = EmptyChangeListener()

        val childRouter =
            controller1.getChildRouter(controller1.childContainer1!!)
        assertTrue(childRouter.getAllRouterListeners(false).contains(routerRecursiveListener))
        assertFalse(childRouter.getAllRouterListeners(false).contains(routerNonRecursiveListener))

        val controller2 = TestController()
        childRouter.addListener(childRouterRecursiveListener, true)
        childRouter.addListener(childRouterNonRecursiveListener)
        childRouter.setRoot(controller2.toTransaction())

        val childRouter2 =
            controller2.getChildRouter(controller2.childContainer2!!)
        val controller3 = TestController()
        childRouter2.push(controller3.toTransaction())
        assertTrue(childRouter2.getAllRouterListeners(false).contains(routerRecursiveListener))
        assertTrue(childRouter2.getAllRouterListeners(false).contains(childRouterRecursiveListener))
        assertFalse(
            childRouter2.getAllRouterListeners(false).contains(
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
        router.setRoot(controller1.toTransaction())

        val childRouterRecursiveListener = EmptyControllerListener()
        val childRouterNonRecursiveListener = EmptyControllerListener()

        val childRouter =
            controller1.getChildRouter(controller1.childContainer1!!)
        assertTrue(childRouter.getAllControllerListeners(false).contains(routerRecursiveListener))
        assertFalse(childRouter.getAllControllerListeners(false).contains(routerNonRecursiveListener))

        val controller2 = TestController()
        childRouter.addControllerListener(childRouterRecursiveListener, true)
        childRouter.addControllerListener(childRouterNonRecursiveListener)
        childRouter.setRoot(controller2.toTransaction())

        val childRouter2 =
            controller2.getChildRouter(controller2.childContainer2!!)
        val controller3 = TestController()
        childRouter2.push(controller3.toTransaction())
        assertTrue(childRouter2.getAllControllerListeners(false).contains(routerRecursiveListener))
        assertTrue(
            childRouter2.getAllControllerListeners(false).contains(
                childRouterRecursiveListener
            )
        )
        assertFalse(
            childRouter2.getAllControllerListeners(false).contains(
                childRouterNonRecursiveListener
            )
        )
    }
}