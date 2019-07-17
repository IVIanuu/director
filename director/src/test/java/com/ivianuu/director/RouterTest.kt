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

import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.EmptyChangeListener
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
    private val router = activityProxy.activity.router(activityProxy.view1)

    @Test
    fun testSetRoot() {
        val rootTag = "root"

        val rootController = TestController()

        assertFalse(router.hasRoot)

        router.setRoot(rootController.toTransaction().tag(rootTag))

        assertTrue(router.hasRoot)

        assertEquals(rootController, router.findControllerByTag(rootTag))
    }

    @Test
    fun testSetNewRoot() {
        val oldRootTag = "oldRoot"
        val newRootTag = "newRoot"

        val oldRootController = TestController()
        val newRootController = TestController()

        router.setRoot(oldRootController.toTransaction().tag(oldRootTag))
        router.setRoot(newRootController.toTransaction().tag(newRootTag))

        assertNull(router.findControllerByTag(oldRootTag))
        assertEquals(newRootController, router.findControllerByTag(newRootTag))
    }

    @Test
    fun testGetByTag() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"

        val controller1 = TestController()
        val controller2 = TestController()

        router.push(controller1.toTransaction().tag(controller1Tag))
        router.push(controller2.toTransaction().tag(controller2Tag))

        assertEquals(controller1, router.findControllerByTag(controller1Tag))
        assertEquals(controller2, router.findControllerByTag(controller2Tag))
    }

    @Test
    fun testPushPopControllers() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"

        val controller1 = TestController()
        val controller2 = TestController()

        router.push(controller1.toTransaction().tag(controller1Tag))

        assertEquals(1, router.backStack.size)

        router.push(controller2.toTransaction().tag(controller2Tag))

        assertEquals(2, router.backStack.size)

        router.popTop()

        assertEquals(1, router.backStack.size)

        assertEquals(controller1, router.findControllerByTag(controller1Tag))
        assertNull(router.findControllerByTag(controller2Tag))

        router.popTop()

        assertEquals(0, router.backStack.size)

        assertNull(router.findControllerByTag(controller1Tag))
        assertNull(router.findControllerByTag(controller2Tag))
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
            val controller = router.findControllerByTag(tag)
            if (controller != null) {
                router.popController(controller)
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

        assertEquals(2, router.backStack.size)
        assertEquals(controller1, router.findControllerByTag(controller1Tag))
        assertEquals(controller2, router.findControllerByTag(controller2Tag))
        assertNull(router.findControllerByTag(controller3Tag))
        assertNull(router.findControllerByTag(controller4Tag))
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

        router.popController(controller2)

        assertEquals(2, router.backStack.size)
        assertEquals(controller1, router.findControllerByTag(controller1Tag))
        assertNull(router.findControllerByTag(controller2Tag))
        assertEquals(controller3, router.findControllerByTag(controller3Tag))
    }

    @Test
    fun testSetBackStack() {
        router.setRoot(TestController().toTransaction())

        assertEquals(1, router.backStack.size)

        val rootTransaction = TestController().toTransaction()
        val middleTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()

        val backStack =
            listOf(rootTransaction, middleTransaction, topTransaction)
        router.setBackStack(backStack, true)

        assertEquals(3, router.backStack.size)

        val fetchedBackStack = router.backStack
        assertEquals(rootTransaction, fetchedBackStack[0])
        assertEquals(middleTransaction, fetchedBackStack[1])
        assertEquals(topTransaction, fetchedBackStack[2])

        assertEquals(router, rootTransaction.controller.router)
        assertEquals(router, middleTransaction.controller.router)
        assertEquals(router, topTransaction.controller.router)
    }

    @Test
    fun testSetBackStackWithNoRemoveViewOnPush() {
        val oldRootTransaction = TestController().toTransaction()
        val oldTopTransaction = TestController().toTransaction()
            .pushChangeHandler(noRemoveViewOnPushHandler())

        router.setRoot(oldRootTransaction)
        router.push(oldTopTransaction)
        assertEquals(2, router.backStack.size)

        assertTrue(oldRootTransaction.controller.lifecycle.currentState == RESUMED)
        assertTrue(oldTopTransaction.controller.lifecycle.currentState == RESUMED)

        val rootTransaction = TestController().toTransaction()
        val middleTransaction = TestController().toTransaction()
            .pushChangeHandler(noRemoveViewOnPushHandler())
        val topTransaction = TestController().toTransaction()
            .pushChangeHandler(noRemoveViewOnPushHandler())

        val backStack = listOf(rootTransaction, middleTransaction, topTransaction)
        router.setBackStack(backStack, true)

        assertEquals(3, router.backStack.size)

        val fetchedBackStack = router.backStack
        assertEquals(rootTransaction, fetchedBackStack[0])
        assertEquals(middleTransaction, fetchedBackStack[1])
        assertEquals(topTransaction, fetchedBackStack[2])

        assertFalse(oldRootTransaction.controller.lifecycle.currentState == RESUMED)
        assertFalse(oldTopTransaction.controller.lifecycle.currentState == RESUMED)
        assertTrue(rootTransaction.controller.lifecycle.currentState == RESUMED)
        assertTrue(middleTransaction.controller.lifecycle.currentState == RESUMED)
        assertTrue(topTransaction.controller.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testPopToRoot() {
        val rootTransaction = TestController().toTransaction()
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        val backStack =
            listOf(rootTransaction, transaction1, transaction2)
        router.setBackStack(backStack, true)

        assertEquals(3, router.backStack.size)

        router.popToRoot()

        assertEquals(1, router.backStack.size)
        assertEquals(rootTransaction, router.backStack[0])

        assertTrue(rootTransaction.controller.lifecycle.currentState == RESUMED)
        assertFalse(transaction1.controller.lifecycle.currentState == RESUMED)
        assertFalse(transaction2.controller.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testPopToRootWithNoRemoveViewOnPush() {
        val rootTransaction = TestController().toTransaction()
            .pushChangeHandler(DefaultChangeHandler(false))
        val transaction1 = TestController().toTransaction()
            .pushChangeHandler(DefaultChangeHandler(false))
        val transaction2 = TestController().toTransaction()
            .pushChangeHandler(DefaultChangeHandler(false))

        val backStack =
            listOf(rootTransaction, transaction1, transaction2)
        router.setBackStack(backStack, true)

        assertEquals(3, router.backStack.size)

        router.popToRoot()

        assertEquals(1, router.backStack.size)
        assertEquals(rootTransaction, router.backStack[0])

        assertTrue(rootTransaction.controller.lifecycle.currentState == RESUMED)
        assertFalse(transaction1.controller.lifecycle.currentState == RESUMED)
        assertFalse(transaction2.controller.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testReplaceTopController() {
        val rootTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()

        val backStack = listOf(rootTransaction, topTransaction)
        router.setBackStack(backStack, true)

        assertEquals(2, router.backStack.size)

        var fetchedBackStack = router.backStack
        assertEquals(rootTransaction, fetchedBackStack[0])
        assertEquals(topTransaction, fetchedBackStack[1])

        val newTopTransaction = TestController().toTransaction()
        router.replaceTop(newTopTransaction)

        assertEquals(2, router.backStack.size)

        fetchedBackStack = router.backStack
        assertEquals(rootTransaction, fetchedBackStack[0])
        assertEquals(newTopTransaction, fetchedBackStack[1])
    }

    @Test
    fun testReplaceTopControllerWithNoRemoveViewOnPush() {
        val controllerA = TestController().toTransaction()
        val controllerB = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())

        val backStack = listOf(controllerA, controllerB)
        router.setBackStack(backStack, true)

        assertEquals(2, router.backStack.size)

        assertTrue(controllerA.controller.lifecycle.currentState == RESUMED)
        assertTrue(controllerB.controller.lifecycle.currentState == RESUMED)

        val controllerC = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())
        router.replaceTop(controllerC)

        assertEquals(2, router.backStack.size)

        assertTrue(controllerA.controller.lifecycle.currentState == RESUMED)
        assertFalse(controllerB.controller.lifecycle.currentState == RESUMED)
        assertTrue(controllerC.controller.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testReplaceTopControllerWithMixedRemoveViewOnPush1() {
        val controllerA = TestController().toTransaction()
        val controllerB = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())

        val backStack = listOf(controllerA, controllerB)
        router.setBackStack(backStack, true)

        assertEquals(2, router.backStack.size)

        assertTrue(controllerA.controller.lifecycle.currentState == RESUMED)
        assertTrue(controllerB.controller.lifecycle.currentState == RESUMED)

        val controllerC = TestController().toTransaction()
        router.replaceTop(controllerC)

        assertEquals(2, router.backStack.size)

        assertFalse(controllerA.controller.lifecycle.currentState == RESUMED)
        assertFalse(controllerB.controller.lifecycle.currentState == RESUMED)
        assertTrue(controllerC.controller.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testReplaceTopControllerWithMixedRemoveViewOnPush2() {
        val controllerA = TestController().toTransaction()
        val controllerB = TestController().toTransaction()

        val backStack = listOf(controllerA, controllerB)
        router.setBackStack(backStack, true)

        assertEquals(2, router.backStack.size)

        assertFalse(controllerA.controller.lifecycle.currentState == RESUMED)
        assertTrue(controllerB.controller.lifecycle.currentState == RESUMED)

        val controllerC = TestController().toTransaction()
            .changeHandler(noRemoveViewOnPushHandler())
        router.replaceTop(controllerC)

        assertEquals(2, router.backStack.size)

        assertTrue(controllerA.controller.lifecycle.currentState == RESUMED)
        assertFalse(controllerB.controller.lifecycle.currentState == RESUMED)
        assertTrue(controllerC.controller.lifecycle.currentState == RESUMED)
    }

    @Test
    fun testAddBetweenWithNoRemoveView() {
        // todo implement
    }

    @Test
    fun testSettingSameBackStackNoOps() {
        var changeCalls = 0
        router.doOnChangeStarted { _, _, _, _, _, _ -> changeCalls++ }

        val backStack = listOf(
            TestController().toTransaction(),
            TestController().toTransaction(),
            TestController().toTransaction()
        )

        router.setBackStack(backStack, true)
        assertEquals(1, changeCalls)

        router.setBackStack(backStack, true)
        assertEquals(1, changeCalls)
    }

    @Test
    fun testSettingSameVisibleControllersNoOps() {
        var changeCalls = 0
        router.doOnChangeStarted { _, _, _, _, _, _ -> changeCalls++ }

        val backStack = listOf(
            TestController().toTransaction(),
            TestController().toTransaction(),
            TestController().toTransaction()
                .changeHandler(noRemoveViewOnPushHandler())
        )

        router.setBackStack(backStack, true)
        assertEquals(2, changeCalls)

        router.setBackStack(backStack.toMutableList().apply { removeAt(0) }, true)
        assertEquals(2, changeCalls)
    }

    @Test
    fun testRearrangeTransactionBackStack() {
        router.popsLastView = true
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        var backStack = listOf(transaction1, transaction2)
        router.setBackStack(backStack, true)

        backStack = listOf(transaction2, transaction1)
        router.setBackStack(backStack, true)

        router.handleBack()

        assertEquals(1, router.backStack.size)
        assertEquals(transaction2, router.backStack[0])

        router.handleBack()
        assertEquals(0, router.backStack.size)
    }

    @Test
    fun testRecursivelySettingRouterListener() {
        val routerRecursiveListener = EmptyChangeListener()
        val routerNonRecursiveListener = EmptyChangeListener()

        val controller1 = TestController()
        router.addChangeListener(routerRecursiveListener, true)
        router.addChangeListener(routerNonRecursiveListener)
        router.setRoot(controller1.toTransaction())

        val childRouterRecursiveListener = EmptyChangeListener()
        val childRouterNonRecursiveListener = EmptyChangeListener()

        val childRouter =
            controller1.childRouter(controller1.childContainer1!!)
        assertTrue(childRouter.getChangeListeners(false).contains(routerRecursiveListener))
        assertFalse(childRouter.getChangeListeners(false).contains(routerNonRecursiveListener))

        val controller2 = TestController()
        childRouter.addChangeListener(childRouterRecursiveListener, true)
        childRouter.addChangeListener(childRouterNonRecursiveListener)
        childRouter.setRoot(controller2.toTransaction())

        val childRouter2 =
            controller2.childRouter(controller2.childContainer2!!)
        val controller3 = TestController()
        childRouter2.push(controller3.toTransaction())
        assertTrue(childRouter2.getChangeListeners(false).contains(routerRecursiveListener))
        assertTrue(childRouter2.getChangeListeners(false).contains(childRouterRecursiveListener))
        assertFalse(
            childRouter2.getChangeListeners(false).contains(
                childRouterNonRecursiveListener
            )
        )
    }

}