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
import com.ivianuu.director.util.MockChangeHandler
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RouterChangeHandlerTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getOrCreateRouter(activityProxy.view)

    @Test
    fun testSetRootHandler() {
        val handler = MockChangeHandler.taggedHandler("root", true)
        val rootController = TestController()
        router.setRoot(rootController, pushHandler = handler)

        assertTrue(rootController.changeHandlerHistory.isValidHistory)
        assertNull(rootController.changeHandlerHistory.latestFromView())
        assertNotNull(rootController.changeHandlerHistory.latestToView())
        assertEquals(rootController.view, rootController.changeHandlerHistory.latestToView())
        assertTrue(rootController.changeHandlerHistory.latestIsPush())
        assertEquals(handler.tag, rootController.changeHandlerHistory.latestChangeHandler().tag)
    }

    @Test
    fun testPushPopHandlers() {
        val rootController = TestController()
        router.setRoot(rootController, pushHandler = MockChangeHandler.defaultHandler())
        val rootView = rootController.view

        val pushHandler = MockChangeHandler.taggedHandler("push", true)
        val popHandler = MockChangeHandler.taggedHandler("pop", true)
        val pushController = TestController()
        router.push {
            controller(pushController)
            pushHandler(pushHandler)
            popHandler(popHandler)
        }

        assertTrue(rootController.changeHandlerHistory.isValidHistory)
        assertTrue(pushController.changeHandlerHistory.isValidHistory)

        assertNotNull(pushController.changeHandlerHistory.latestFromView())
        assertNotNull(pushController.changeHandlerHistory.latestToView())
        assertEquals(rootView, pushController.changeHandlerHistory.latestFromView())
        assertEquals(pushController.view, pushController.changeHandlerHistory.latestToView())
        assertTrue(pushController.changeHandlerHistory.latestIsPush())
        assertEquals(pushHandler.tag, pushController.changeHandlerHistory.latestChangeHandler().tag)

        val pushView = pushController.view
        router.pop(pushController)

        assertNotNull(pushController.changeHandlerHistory.latestFromView())
        assertNotNull(pushController.changeHandlerHistory.latestToView())
        assertEquals(pushView, pushController.changeHandlerHistory.fromViewAt(1))
        assertEquals(rootController.view, pushController.changeHandlerHistory.latestToView())
        assertFalse(pushController.changeHandlerHistory.latestIsPush())
        assertEquals(popHandler.tag, pushController.changeHandlerHistory.latestChangeHandler().tag)
    }

    @Test
    fun testResetRootHandlers() {
        val initialController1 = TestController()
        val initialPushHandler1 = MockChangeHandler.taggedHandler("initialPush1", true)
        val initialPopHandler1 = MockChangeHandler.taggedHandler("initialPop1", true)
        router.setRoot {
            controller(initialController1)
            pushHandler(initialPushHandler1)
            popHandler(initialPopHandler1)
        }

        val initialController2 = TestController()
        val initialPushHandler2 = MockChangeHandler.taggedHandler("initialPush2", false)
        val initialPopHandler2 = MockChangeHandler.taggedHandler("initialPop2", false)
        router.push {
            controller(initialController2)
            pushHandler(initialPushHandler2)
            popHandler(initialPopHandler2)
        }

        val initialView1 = initialController1.view
        val initialView2 = initialController2.view

        val newRootController = TestController()
        val newRootHandler = MockChangeHandler.taggedHandler("newRootHandler", true)

        router.setRoot(newRootController, pushHandler = newRootHandler)

        assertTrue(initialController1.changeHandlerHistory.isValidHistory)
        assertTrue(initialController2.changeHandlerHistory.isValidHistory)
        assertTrue(newRootController.changeHandlerHistory.isValidHistory)

        assertEquals(3, initialController1.changeHandlerHistory.size())
        assertEquals(2, initialController2.changeHandlerHistory.size())
        assertEquals(1, newRootController.changeHandlerHistory.size())

        assertNotNull(initialController1.changeHandlerHistory.latestToView())
        assertEquals(
            newRootController.view,
            initialController1.changeHandlerHistory.latestToView()
        )
        assertEquals(initialView1, initialController1.changeHandlerHistory.latestFromView())
        assertEquals(
            newRootHandler.tag,
            initialController1.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController1.changeHandlerHistory.latestIsPush())

        assertNull(initialController2.changeHandlerHistory.latestToView())
        assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
        assertEquals(
            newRootHandler.tag,
            initialController2.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController2.changeHandlerHistory.latestIsPush())

        assertNotNull(newRootController.changeHandlerHistory.latestToView())
        assertEquals(
            newRootController.view,
            newRootController.changeHandlerHistory.latestToView()
        )
        assertEquals(initialView1, newRootController.changeHandlerHistory.latestFromView())
        assertEquals(
            newRootHandler.tag,
            newRootController.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(newRootController.changeHandlerHistory.latestIsPush())
    }

    @Test
    fun testSetBackstackHandlers() {
        val initialController1 = TestController()
        val initialPushHandler1 = MockChangeHandler.taggedHandler("initialPush1", true)
        val initialPopHandler1 = MockChangeHandler.taggedHandler("initialPop1", true)
        router.setRoot(
            initialController1,
            pushHandler = initialPushHandler1, popHandler = initialPopHandler1
        )

        val initialController2 = TestController()
        val initialPushHandler2 = MockChangeHandler.taggedHandler("initialPush2", false)
        val initialPopHandler2 = MockChangeHandler.taggedHandler("initialPop2", false)
        router.push(
            initialController2,
            pushHandler = initialPushHandler2, popHandler = initialPopHandler2
        )

        val initialView1 = initialController1.view
        val initialView2 = initialController2.view

        val newController1 = TestController()
        val newController2 = TestController()
        val setBackstackHandler = MockChangeHandler.taggedHandler("setBackstackHandler", true)

        val newBackstack = listOf(
            newController1.toTransaction(),
            newController2.toTransaction()
        )

        router.setBackstack(newBackstack, setBackstackHandler)

        assertTrue(initialController1.changeHandlerHistory.isValidHistory)
        assertTrue(initialController2.changeHandlerHistory.isValidHistory)
        assertTrue(newController1.changeHandlerHistory.isValidHistory)

        assertEquals(3, initialController1.changeHandlerHistory.size())
        assertEquals(2, initialController2.changeHandlerHistory.size())
        assertEquals(0, newController1.changeHandlerHistory.size())
        assertEquals(1, newController2.changeHandlerHistory.size())
        assertNotNull(initialController1.changeHandlerHistory.latestToView())
        assertEquals(
            newController2.view,
            initialController1.changeHandlerHistory.latestToView()
        )
        assertEquals(initialView1, initialController1.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController1.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController1.changeHandlerHistory.latestIsPush())

        assertNull(initialController2.changeHandlerHistory.latestToView())
        assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController2.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController2.changeHandlerHistory.latestIsPush())

        assertNotNull(newController2.changeHandlerHistory.latestToView())
        assertEquals(newController2.view, newController2.changeHandlerHistory.latestToView())
        assertEquals(initialView1, newController2.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            newController2.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(newController2.changeHandlerHistory.latestIsPush())
    }

    @Test
    fun testSetBackstackWithTwoVisibleHandlers() {
        val initialController1 = TestController()
        val initialPushHandler1 = MockChangeHandler.taggedHandler("initialPush1", true)
        val initialPopHandler1 = MockChangeHandler.taggedHandler("initialPop1", true)
        router.setRoot(initialController1, initialPushHandler1, initialPopHandler1)

        val initialController2 = TestController()
        val initialPushHandler2 = MockChangeHandler.taggedHandler("initialPush2", false)
        val initialPopHandler2 = MockChangeHandler.taggedHandler("initialPop2", false)
        router.push(initialController2, initialPushHandler2, initialPopHandler2)

        val initialView1 = initialController1.view
        val initialView2 = initialController2.view

        val newController1 = TestController()
        val newController2 = TestController()
        val setBackstackHandler = MockChangeHandler.taggedHandler("setBackstackHandler", true)
        val pushController2Handler = MockChangeHandler.noRemoveViewOnPushHandler("pushController2")
        val newBackstack = listOf(
            newController1.toTransaction(),
            newController2.toTransaction(pushController2Handler)
        )

        router.setBackstack(newBackstack, setBackstackHandler)

        assertTrue(initialController1.changeHandlerHistory.isValidHistory)
        assertTrue(initialController2.changeHandlerHistory.isValidHistory)
        assertTrue(newController1.changeHandlerHistory.isValidHistory)

        assertEquals(3, initialController1.changeHandlerHistory.size())
        assertEquals(2, initialController2.changeHandlerHistory.size())
        assertEquals(2, newController1.changeHandlerHistory.size())
        assertEquals(1, newController2.changeHandlerHistory.size())

        assertNotNull(initialController1.changeHandlerHistory.latestToView())
        assertEquals(
            newController1.view,
            initialController1.changeHandlerHistory.latestToView()
        )
        assertEquals(initialView1, initialController1.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController1.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController1.changeHandlerHistory.latestIsPush())

        assertNull(initialController2.changeHandlerHistory.latestToView())
        assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController2.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController2.changeHandlerHistory.latestIsPush())

        assertNotNull(newController1.changeHandlerHistory.latestToView())
        assertEquals(newController1.view, newController1.changeHandlerHistory.toViewAt(0))
        assertEquals(newController2.view, newController1.changeHandlerHistory.latestToView())
        assertEquals(initialView1, newController1.changeHandlerHistory.fromViewAt(0))
        assertEquals(newController1.view, newController1.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            newController1.changeHandlerHistory.changeHandlerAt(0).tag
        )
        assertEquals(
            pushController2Handler.tag,
            newController1.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(newController1.changeHandlerHistory.latestIsPush())

        assertNotNull(newController2.changeHandlerHistory.latestToView())
        assertEquals(newController2.view, newController2.changeHandlerHistory.latestToView())
        assertEquals(newController1.view, newController2.changeHandlerHistory.latestFromView())
        assertEquals(
            pushController2Handler.tag,
            newController2.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(newController2.changeHandlerHistory.latestIsPush())
    }

    @Test
    fun testSetBackstackForPushHandlers() {
        val initialController = TestController()
        val initialPushHandler = MockChangeHandler.taggedHandler("initialPush1", true)
        val initialPopHandler = MockChangeHandler.taggedHandler("initialPop1", true)
        val initialTransaction = initialController
            .toTransaction(initialPushHandler, initialPopHandler)

        router.setRoot(initialTransaction)
        val initialView = initialController.view

        val newController = TestController()
        val setBackstackHandler = MockChangeHandler.taggedHandler("setBackstackHandler", true)

        val newBackstack = listOf(
            initialTransaction,
            newController.toTransaction()
        )

        router.setBackstack(newBackstack, setBackstackHandler)

        assertTrue(initialController.changeHandlerHistory.isValidHistory)
        assertTrue(newController.changeHandlerHistory.isValidHistory)

        assertEquals(2, initialController.changeHandlerHistory.size())
        assertEquals(1, newController.changeHandlerHistory.size())

        assertNotNull(initialController.changeHandlerHistory.latestToView())
        assertEquals(newController.view, initialController.changeHandlerHistory.latestToView())
        assertEquals(initialView, initialController.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController.changeHandlerHistory.latestIsPush())
        assertTrue(newController.changeHandlerHistory.latestIsPush())
    }

    @Test
    fun testSetBackstackForInvertHandlersWithRemovesView() {
        val initialController1 = TestController()
        val initialPushHandler1 = MockChangeHandler.taggedHandler("initialPush1", true)
        val initialPopHandler1 = MockChangeHandler.taggedHandler("initialPop1", true)
        val initialTransaction1 = initialController1.toTransaction(
            initialPushHandler1, initialPopHandler1
        )
        router.setRoot(initialTransaction1)

        val initialController2 = TestController()
        val initialPushHandler2 = MockChangeHandler.taggedHandler("initialPush2", true)
        val initialPopHandler2 = MockChangeHandler.taggedHandler("initialPop2", true)
        val initialTransaction2 = initialController2.toTransaction(
            initialPushHandler2, initialPopHandler2
        )
        router.push(initialTransaction2)

        val initialView2 = initialController2.view

        val setBackstackHandler = MockChangeHandler.taggedHandler("setBackstackHandler", true)
        val newBackstack = listOf(
            initialTransaction2,
            initialTransaction1
        )

        router.setBackstack(newBackstack, setBackstackHandler)

        assertTrue(initialController1.changeHandlerHistory.isValidHistory)
        assertTrue(initialController2.changeHandlerHistory.isValidHistory)

        assertEquals(3, initialController1.changeHandlerHistory.size())
        assertEquals(2, initialController2.changeHandlerHistory.size())

        assertNotNull(initialController1.changeHandlerHistory.latestToView())
        assertEquals(initialView2, initialController1.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController1.changeHandlerHistory.latestChangeHandler().tag
        )
        assertFalse(initialController1.changeHandlerHistory.latestIsPush())

        assertNotNull(initialController2.changeHandlerHistory.latestToView())
        assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController2.changeHandlerHistory.latestChangeHandler().tag
        )
        assertFalse(initialController2.changeHandlerHistory.latestIsPush())
    }

    @Test
    fun testSetBackstackForInvertHandlersWithoutRemovesView() {
        val initialController1 = TestController()
        val initialPushHandler1 = MockChangeHandler.taggedHandler("initialPush1", true)
        val initialPopHandler1 = MockChangeHandler.taggedHandler("initialPop1", true)
        val initialTransaction1 = initialController1.toTransaction(
            initialPushHandler1, initialPopHandler1
        )
        router.setRoot(initialTransaction1)

        val initialController2 = TestController()
        val initialPushHandler2 = MockChangeHandler.taggedHandler("initialPush2", false)
        val initialPopHandler2 = MockChangeHandler.taggedHandler("initialPop2", false)
        val initialTransaction2 = initialController2.toTransaction(
            initialPushHandler2, initialPopHandler2
        )
        router.push(initialTransaction2)

        val initialView1 = initialController1.view
        val initialView2 = initialController2.view

        val setBackstackHandler = MockChangeHandler.taggedHandler("setBackstackHandler", true)
        val newBackstack = listOf(
            initialTransaction2,
            initialTransaction1
        )

        router.setBackstack(newBackstack, setBackstackHandler)

        assertTrue(initialController1.changeHandlerHistory.isValidHistory)
        assertTrue(initialController2.changeHandlerHistory.isValidHistory)

        assertEquals(2, initialController1.changeHandlerHistory.size())
        assertEquals(2, initialController2.changeHandlerHistory.size())

        assertNotNull(initialController1.changeHandlerHistory.latestToView())
        assertEquals(initialView1, initialController1.changeHandlerHistory.latestFromView())
        assertEquals(
            initialPushHandler2.tag,
            initialController1.changeHandlerHistory.latestChangeHandler().tag
        )
        assertTrue(initialController1.changeHandlerHistory.latestIsPush())

        assertNull(initialController2.changeHandlerHistory.latestToView())
        assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
        assertEquals(
            setBackstackHandler.tag,
            initialController2.changeHandlerHistory.latestChangeHandler().tag
        )
        assertFalse(initialController2.changeHandlerHistory.latestIsPush())
    }
}