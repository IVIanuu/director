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

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.EmptyChangeListener
import com.ivianuu.director.util.EmptyLifecycleListener
import com.ivianuu.director.util.MockChangeHandler
import com.ivianuu.director.util.TestController
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
    private val router = activityProxy.activity.attachRouter(activityProxy.view)

    @Test
    fun testSetRoot() {
        val rootTag = "root"

        val rootController = TestController()

        assertFalse(router.hasRootController)

        router.setRoot(rootController.toTransaction().tag(rootTag))

        assertTrue(router.hasRootController)

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
    fun testGetByInstanceId() {
        val controller = TestController()

        router.pushController(controller.toTransaction())

        assertEquals(controller, router.findControllerByInstanceId(controller.instanceId))
        assertNull(router.findControllerByInstanceId("fake id"))
    }

    @Test
    fun testGetByTag() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"

        val controller1 = TestController()
        val controller2 = TestController()

        router.pushController(
            controller1.toTransaction()
                .tag(controller1Tag)
        )

        router.pushController(
            controller2.toTransaction()
                .tag(controller2Tag)
        )

        assertEquals(controller1, router.findControllerByTag(controller1Tag))
        assertEquals(controller2, router.findControllerByTag(controller2Tag))
    }

    @Test
    fun testPushPopControllers() {
        val controller1Tag = "controller1"
        val controller2Tag = "controller2"

        val controller1 = TestController()
        val controller2 = TestController()

        router.pushController(
            controller1.toTransaction()
                .tag(controller1Tag)
        )

        assertEquals(1, router.backstack.size)

        router.pushController(
            controller2.toTransaction()
                .tag(controller2Tag)
        )

        assertEquals(2, router.backstack.size)

        router.popCurrentController()

        assertEquals(1, router.backstack.size)

        assertEquals(controller1, router.findControllerByTag(controller1Tag))
        assertNull(router.findControllerByTag(controller2Tag))

        router.popCurrentController()

        assertEquals(0, router.backstack.size)

        assertNull(router.findControllerByTag(controller1Tag))
        assertNull(router.findControllerByTag(controller2Tag))
    }

    @Test
    fun testPopControllerConcurrentModificationException() {
        var step = 1
        var i = 0
        while (i < 10) {
            router.pushController(TestController().toTransaction().tag("1"))
            router.pushController(TestController().toTransaction().tag("2"))
            router.pushController(TestController().toTransaction().tag("3"))

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

        router.pushController(
            controller1.toTransaction()
                .tag(controller1Tag)
        )

        router.pushController(
            controller2.toTransaction()
                .tag(controller2Tag)
        )

        router.pushController(
            controller3.toTransaction()
                .tag(controller3Tag)
        )

        router.pushController(
            controller4.toTransaction()
                .tag(controller4Tag)
        )

        router.popToTag(controller2Tag)

        assertEquals(2, router.backstack.size)
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

        router.pushController(
            controller1.toTransaction()
                .tag(controller1Tag)
        )

        router.pushController(
            controller2.toTransaction()
                .tag(controller2Tag)
        )

        router.pushController(
            controller3.toTransaction()
                .tag(controller3Tag)
        )

        router.popController(controller2)

        assertEquals(2, router.backstack.size)
        assertEquals(controller1, router.findControllerByTag(controller1Tag))
        assertNull(router.findControllerByTag(controller2Tag))
        assertEquals(controller3, router.findControllerByTag(controller3Tag))
    }

    @Test
    fun testSetBackstack() {
        val rootTransaction = TestController().toTransaction()
        val middleTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()

        val backstack =
            listOf(rootTransaction, middleTransaction, topTransaction)
        router.setBackstack(backstack)

        assertEquals(3, router.backstack.size)

        val fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(middleTransaction, fetchedBackstack[1])
        assertEquals(topTransaction, fetchedBackstack[2])
    }

    @Test
    fun testNewSetBackstack() {
        router.setRoot(TestController().toTransaction())

        assertEquals(1, router.backstack.size)

        val rootTransaction = TestController().toTransaction()
        val middleTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()

        val backstack =
            listOf(rootTransaction, middleTransaction, topTransaction)
        router.setBackstack(backstack)

        assertEquals(3, router.backstack.size)

        val fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(middleTransaction, fetchedBackstack[1])
        assertEquals(topTransaction, fetchedBackstack[2])

        assertEquals(router, rootTransaction.controller.router)
        assertEquals(router, middleTransaction.controller.router)
        assertEquals(router, topTransaction.controller.router)
    }

    @Test
    fun testNewSetBackstackWithNoRemoveViewOnPush() {
        val oldRootTransaction = TestController().toTransaction()
        val oldTopTransaction = TestController().toTransaction()
            .pushChangeHandler(MockChangeHandler.noRemoveViewOnPushHandler())

        router.setRoot(oldRootTransaction)
        router.pushController(oldTopTransaction)
        assertEquals(2, router.backstack.size)

        assertTrue(oldRootTransaction.controller.state == ControllerState.ATTACHED)
        assertTrue(oldTopTransaction.controller.state == ControllerState.ATTACHED)

        val rootTransaction = TestController().toTransaction()
        val middleTransaction = TestController().toTransaction()
            .pushChangeHandler(MockChangeHandler.noRemoveViewOnPushHandler())
        val topTransaction = TestController().toTransaction()
            .pushChangeHandler(MockChangeHandler.noRemoveViewOnPushHandler())

        val backstack = listOf(rootTransaction, middleTransaction, topTransaction)
        router.setBackstack(backstack)

        assertEquals(3, router.backstack.size)

        val fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(middleTransaction, fetchedBackstack[1])
        assertEquals(topTransaction, fetchedBackstack[2])

        assertFalse(oldRootTransaction.controller.state == ControllerState.ATTACHED)
        assertFalse(oldTopTransaction.controller.state == ControllerState.ATTACHED)
        assertTrue(rootTransaction.controller.state == ControllerState.ATTACHED)
        assertTrue(middleTransaction.controller.state == ControllerState.ATTACHED)
        assertTrue(topTransaction.controller.state == ControllerState.ATTACHED)
    }

    @Test
    fun testPopToRoot() {
        val rootTransaction = TestController().toTransaction()
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        val backstack =
            listOf(rootTransaction, transaction1, transaction2)
        router.setBackstack(backstack)

        assertEquals(3, router.backstack.size)

        router.popToRoot()

        assertEquals(1, router.backstack.size)
        assertEquals(rootTransaction, router.backstack[0])

        assertTrue(rootTransaction.controller.state == ControllerState.ATTACHED)
        assertFalse(transaction1.controller.state == ControllerState.ATTACHED)
        assertFalse(transaction2.controller.state == ControllerState.ATTACHED)
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
        router.setBackstack(backstack)

        assertEquals(3, router.backstack.size)

        router.popToRoot()

        assertEquals(1, router.backstack.size)
        assertEquals(rootTransaction, router.backstack[0])

        assertTrue(rootTransaction.controller.state == ControllerState.ATTACHED)
        assertFalse(transaction1.controller.state == ControllerState.ATTACHED)
        assertFalse(transaction2.controller.state == ControllerState.ATTACHED)
    }

    @Test
    fun testReplaceTopController() {
        val rootTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()

        val backstack = listOf(rootTransaction, topTransaction)
        router.setBackstack(backstack)

        assertEquals(2, router.backstack.size)

        var fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(topTransaction, fetchedBackstack[1])

        val newTopTransaction = TestController().toTransaction()
        router.replaceTopController(newTopTransaction)

        assertEquals(2, router.backstack.size)

        fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(newTopTransaction, fetchedBackstack[1])
    }

    @Test
    fun testReplaceTopControllerWithNoRemoveViewOnPush() {
        val rootTransaction = TestController().toTransaction()
        val topTransaction = TestController().toTransaction()
            .pushChangeHandler(MockChangeHandler.noRemoveViewOnPushHandler())

        val backstack = listOf(rootTransaction, topTransaction)
        router.setBackstack(backstack)

        assertEquals(2, router.backstack.size)

        assertTrue(rootTransaction.controller.state == ControllerState.ATTACHED)
        assertTrue(topTransaction.controller.state == ControllerState.ATTACHED)

        var fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(topTransaction, fetchedBackstack[1])

        val newTopTransaction = TestController().toTransaction()
            .pushChangeHandler(MockChangeHandler.noRemoveViewOnPushHandler())
        router.replaceTopController(newTopTransaction)
        newTopTransaction.pushChangeHandler!!.cancel(true)

        assertEquals(2, router.backstack.size)

        fetchedBackstack = router.backstack
        assertEquals(rootTransaction, fetchedBackstack[0])
        assertEquals(newTopTransaction, fetchedBackstack[1])

        assertTrue(rootTransaction.controller.state == ControllerState.ATTACHED)
        assertFalse(topTransaction.controller.state == ControllerState.ATTACHED)
        assertTrue(newTopTransaction.controller.state == ControllerState.ATTACHED)
    }

    @Test
    fun testRearrangeTransactionBackstack() {
        router.popsLastView = true
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        var backstack = listOf(transaction1, transaction2)
        router.setBackstack(backstack)

        assertEquals(1, transaction1.transactionIndex)
        assertEquals(2, transaction2.transactionIndex)

        backstack = listOf(transaction2, transaction1)
        router.setBackstack(backstack)

        assertEquals(1, transaction2.transactionIndex)
        assertEquals(2, transaction1.transactionIndex)

        router.handleBack()

        assertEquals(1, router.backstack.size)
        assertEquals(transaction2, router.backstack[0])

        router.handleBack()
        assertEquals(0, router.backstack.size)
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
        childRouter.setBackstack(backstack)

        assertEquals(2, transaction1.transactionIndex)
        assertEquals(3, transaction2.transactionIndex)

        backstack = listOf(transaction2, transaction1)
        childRouter.setBackstack(backstack)

        assertEquals(2, transaction2.transactionIndex)
        assertEquals(3, transaction1.transactionIndex)

        childRouter.handleBack()

        assertEquals(1, childRouter.backstack.size)
        assertEquals(transaction2, childRouter.backstack[0])

        childRouter.handleBack()
        assertEquals(0, childRouter.backstack.size)
    }

    @Test
    fun testIsBeingDestroyed() {
        val lifecycleListener = object : ControllerLifecycleListener {
            override fun preUnbindView(controller: Controller, view: View) {
                super.preUnbindView(controller, view)
                assertTrue(controller.isBeingDestroyed)
            }
        }

        val controller1 = TestController()
        val controller2 = TestController()
        controller2.addLifecycleListener(lifecycleListener)

        router.setRoot(controller1.toTransaction())
        router.pushController(controller2.toTransaction())
        assertFalse(controller1.isBeingDestroyed)
        assertFalse(controller2.isBeingDestroyed)

        router.popCurrentController()
        assertFalse(controller1.isBeingDestroyed)
        assertTrue(controller2.isBeingDestroyed)

        val controller3 = TestController()
        controller3.addLifecycleListener(lifecycleListener)
        router.pushController(controller3.toTransaction())
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
        router.addChangeListener(routerRecursiveListener, true)
        router.addChangeListener(routerNonRecursiveListener)
        router.setRoot(controller1.toTransaction())

        val childRouterRecursiveListener = EmptyChangeListener()
        val childRouterNonRecursiveListener = EmptyChangeListener()

        val childRouter =
            controller1.getChildRouter(controller1.childContainer1!!)
        assertTrue(childRouter.getAllChangeListeners(false).contains(routerRecursiveListener))
        assertFalse(childRouter.getAllChangeListeners(false).contains(routerNonRecursiveListener))

        val controller2 = TestController()
        childRouter.addChangeListener(childRouterRecursiveListener, true)
        childRouter.addChangeListener(childRouterNonRecursiveListener)
        childRouter.setRoot(controller2.toTransaction())

        val childRouter2 =
            controller2.getChildRouter(controller2.childContainer2!!)
        val controller3 = TestController()
        childRouter2.pushController(controller3.toTransaction())
        assertTrue(childRouter2.getAllChangeListeners(false).contains(routerRecursiveListener))
        assertTrue(childRouter2.getAllChangeListeners(false).contains(childRouterRecursiveListener))
        assertFalse(
            childRouter2.getAllChangeListeners(false).contains(
                childRouterNonRecursiveListener
            )
        )
    }

    @Test
    fun testRecursivelySettingLifecycleListener() {
        val routerRecursiveListener = EmptyLifecycleListener()
        val routerNonRecursiveListener = EmptyLifecycleListener()

        val controller1 = TestController()
        router.addLifecycleListener(routerRecursiveListener, true)
        router.addLifecycleListener(routerNonRecursiveListener)
        router.setRoot(controller1.toTransaction())

        val childRouterRecursiveListener = EmptyLifecycleListener()
        val childRouterNonRecursiveListener = EmptyLifecycleListener()

        val childRouter =
            controller1.getChildRouter(controller1.childContainer1!!)
        assertTrue(childRouter.getAllLifecycleListeners(false).contains(routerRecursiveListener))
        assertFalse(childRouter.getAllLifecycleListeners(false).contains(routerNonRecursiveListener))

        val controller2 = TestController()
        childRouter.addLifecycleListener(childRouterRecursiveListener, true)
        childRouter.addLifecycleListener(childRouterNonRecursiveListener)
        childRouter.setRoot(controller2.toTransaction())

        val childRouter2 =
            controller2.getChildRouter(controller2.childContainer2!!)
        val controller3 = TestController()
        childRouter2.pushController(controller3.toTransaction())
        assertTrue(childRouter2.getAllLifecycleListeners(false).contains(routerRecursiveListener))
        assertTrue(
            childRouter2.getAllLifecycleListeners(false).contains(
                childRouterRecursiveListener
            )
        )
        assertFalse(
            childRouter2.getAllLifecycleListeners(false).contains(
                childRouterNonRecursiveListener
            )
        )
    }
}