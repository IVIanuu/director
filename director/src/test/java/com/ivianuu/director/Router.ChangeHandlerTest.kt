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

/**
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RouterChangeHandlerTest {

private val activityProxy = ActivityProxy().create(null).start().resume()
private val router = activityProxy.activity.getRouter(activityProxy.view1)

@Test
fun testSetRootHandler() {
val handler = taggedHandler("root", true)
val rootController = TestController()
router.setRoot(rootController.pushChangeHandler(handler))

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
router.setRoot(
rootController
.pushChangeHandler(defaultHandler())
)
val rootView = rootController.view

val pushHandler = taggedHandler("push", true)
val popHandler = taggedHandler("pop", true)
val pushController = TestController()
router.push(
pushController
.pushChangeHandler(pushHandler)
.popChangeHandler(popHandler)
)

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
val initialPushHandler1 = taggedHandler("initialPush1", true)
val initialPopHandler1 = taggedHandler("initialPop1", true)
router.setRoot(
initialController1
.pushChangeHandler(initialPushHandler1)
.popChangeHandler(initialPopHandler1)
)

val initialController2 = TestController()
val initialPushHandler2 = taggedHandler("initialPush2", false)
val initialPopHandler2 = taggedHandler("initialPop2", false)
router.push(
initialController2
.pushChangeHandler(initialPushHandler2)
.popChangeHandler(initialPopHandler2)
)

val initialView1 = initialController1.view
val initialView2 = initialController2.view

val newRootController = TestController()
val newRootHandler = taggedHandler("newRootHandler", true)

router.setRoot(newRootController.changeHandler(newRootHandler))

assertTrue(initialController1.changeHandlerHistory.isValidHistory)
assertTrue(initialController2.changeHandlerHistory.isValidHistory)
assertTrue(newRootController.changeHandlerHistory.isValidHistory)

assertEquals(3, initialController1.changeHandlerHistory.size())
assertEquals(2, initialController2.changeHandlerHistory.size())
assertEquals(1, newRootController.changeHandlerHistory.size())

assertNotNull(initialController2.changeHandlerHistory.latestToView())
assertEquals(
newRootController.view,
initialController2.changeHandlerHistory.latestToView()
)
assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
assertEquals(
newRootHandler.tag,
initialController1.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController2.changeHandlerHistory.latestIsPush())

assertNull(initialController1.changeHandlerHistory.latestToView())
assertEquals(initialView1, initialController1.changeHandlerHistory.latestFromView())
assertEquals(
newRootHandler.tag,
initialController1.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController1.changeHandlerHistory.latestIsPush())

assertNotNull(newRootController.changeHandlerHistory.latestToView())
assertEquals(
newRootController.view,
newRootController.changeHandlerHistory.latestToView()
)
assertEquals(initialView2, newRootController.changeHandlerHistory.latestFromView())
assertEquals(
newRootHandler.tag,
newRootController.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(newRootController.changeHandlerHistory.latestIsPush())
}

@Test
fun testSetBackStackHandlers() {
val initialController1 = TestController()
val initialPushHandler1 = taggedHandler("initialPush1", true)
val initialPopHandler1 = taggedHandler("initialPop1", true)
router.setRoot(
initialController1
.pushChangeHandler(initialPushHandler1)
.popChangeHandler(initialPopHandler1)
)

val initialController2 = TestController()
val initialPushHandler2 = taggedHandler("initialPush2", false)
val initialPopHandler2 = taggedHandler("initialPop2", false)
router.push(
initialController2
.pushChangeHandler(initialPushHandler2)
.popChangeHandler(initialPopHandler2)
)

val initialView1 = initialController1.view
val initialView2 = initialController2.view

val newController1 = TestController()
val newController2 = TestController()
val setBackStackHandler = taggedHandler("setBackStackHandler", true)

val newBackStack = listOf(
newController1,
newController2
)

router.setBackStack(newBackStack, true, setBackStackHandler)

assertTrue(initialController1.changeHandlerHistory.isValidHistory)
assertTrue(initialController2.changeHandlerHistory.isValidHistory)
assertTrue(newController1.changeHandlerHistory.isValidHistory)

assertEquals(3, initialController1.changeHandlerHistory.size())
assertEquals(2, initialController2.changeHandlerHistory.size())
assertEquals(0, newController1.changeHandlerHistory.size())
assertEquals(1, newController2.changeHandlerHistory.size())

assertNotNull(initialController2.changeHandlerHistory.latestToView())
assertEquals(
newController2.view,
initialController2.changeHandlerHistory.latestToView()
)
assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController2.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController2.changeHandlerHistory.latestIsPush())

assertNull(initialController1.changeHandlerHistory.latestToView())
assertEquals(initialView1, initialController1.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController1.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController1.changeHandlerHistory.latestIsPush())

assertNotNull(newController2.changeHandlerHistory.latestToView())
assertEquals(newController2.view, newController2.changeHandlerHistory.latestToView())
assertEquals(initialView2, newController2.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
newController2.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(newController2.changeHandlerHistory.latestIsPush())
}

/*@Test
fun testSetBackStackWithTwoVisibleHandlers() {
val initialController1 = TestController()
val initialPushHandler1 = taggedHandler("initialPush1", true)
val initialPopHandler1 = taggedHandler("initialPop1", true)
router.setRoot(initialController1, initialPushHandler1, initialPopHandler1)

val initialController2 = TestController()
val initialPushHandler2 = taggedHandler("initialPush2", false)
val initialPopHandler2 = taggedHandler("initialPop2", false)
router.push(initialController2, initialPushHandler2, initialPopHandler2)

val initialView1 = initialController1.view
val initialView2 = initialController2.view

val newController1 = TestController()
val newController2 = TestController()
val setBackStackHandler = taggedHandler("setBackStackHandler", true)
val pushController2Handler = noRemoveViewOnPushHandler("pushController2")
val newBackStack = listOf(
newController1,
newController2.toTransaction(pushController2Handler)
)

router.setBackStack(newBackStack, true, setBackStackHandler)

assertTrue(initialController1.changeHandlerHistory.isValidHistory)
assertTrue(initialController2.changeHandlerHistory.isValidHistory)
assertTrue(newController1.changeHandlerHistory.isValidHistory)

assertEquals(3, initialController1.changeHandlerHistory.size())
assertEquals(2, initialController2.changeHandlerHistory.size())
assertEquals(1, newController1.changeHandlerHistory.size())
assertEquals(1, newController2.changeHandlerHistory.size())

assertNotNull(initialController1.changeHandlerHistory.latestToView())
assertEquals(
newController1.view,
initialController1.changeHandlerHistory.latestToView()
)
assertEquals(initialView1, initialController1.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController1.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController1.changeHandlerHistory.latestIsPush())

assertNull(initialController2.changeHandlerHistory.latestToView())
assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController2.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController2.changeHandlerHistory.latestIsPush())

assertNotNull(newController1.changeHandlerHistory.latestToView())
assertEquals(newController1.view, newController1.changeHandlerHistory.toViewAt(0))
assertEquals(newController2.view, newController1.changeHandlerHistory.latestToView())
assertEquals(initialView1, newController1.changeHandlerHistory.fromViewAt(0))
assertEquals(newController1.view, newController1.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
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
}*/

@Test
fun testSetBackStackForPushHandlers() {
val initialController = TestController()
val initialPushHandler = taggedHandler("initialPush1", true)
val initialPopHandler = taggedHandler("initialPop1", true)
val initialController = initialController

.pushChangeHandler(initialPushHandler)
.popChangeHandler(initialPopHandler)

router.setRoot(initialController)
val initialView = initialController.view

val newController = TestController()
val setBackStackHandler = taggedHandler("setBackStackHandler", true)

val newBackStack = listOf(
initialController,
newController
)

router.setBackStack(newBackStack, true, setBackStackHandler)

assertTrue(initialController.changeHandlerHistory.isValidHistory)
assertTrue(newController.changeHandlerHistory.isValidHistory)

assertEquals(2, initialController.changeHandlerHistory.size())
assertEquals(1, newController.changeHandlerHistory.size())

assertNotNull(initialController.changeHandlerHistory.latestToView())
assertEquals(newController.view, initialController.changeHandlerHistory.latestToView())
assertEquals(initialView, initialController.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController.changeHandlerHistory.latestIsPush())
assertTrue(newController.changeHandlerHistory.latestIsPush())
}

@Test
fun testSetBackStackForInvertHandlersWithRemovesView() {
val initialController1 = TestController()
val initialPushHandler1 = taggedHandler("initialPush1", true)
val initialPopHandler1 = taggedHandler("initialPop1", true)
val initialController1 = initialController1
.pushChangeHandler(initialPushHandler1)
.popChangeHandler(initialPopHandler1)
router.setRoot(initialController1)

val initialController2 = TestController()
val initialPushHandler2 = taggedHandler("initialPush2", true)
val initialPopHandler2 = taggedHandler("initialPop2", true)
val initialController2 = initialController2
.pushChangeHandler(initialPushHandler2)
.popChangeHandler(initialPopHandler2)
router.push(initialController2)

val initialView2 = initialController2.view

val setBackStackHandler = taggedHandler("setBackStackHandler", true)
val newBackStack = listOf(
initialController2,
initialController1
)

router.setBackStack(newBackStack, false, setBackStackHandler)

assertTrue(initialController1.changeHandlerHistory.isValidHistory)
assertTrue(initialController2.changeHandlerHistory.isValidHistory)

assertEquals(3, initialController1.changeHandlerHistory.size())
assertEquals(2, initialController2.changeHandlerHistory.size())

assertNotNull(initialController1.changeHandlerHistory.latestToView())
assertEquals(initialView2, initialController1.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController1.changeHandlerHistory.latestChangeHandler().tag
)
assertFalse(initialController1.changeHandlerHistory.latestIsPush())

assertNotNull(initialController2.changeHandlerHistory.latestToView())
assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController2.changeHandlerHistory.latestChangeHandler().tag
)
assertFalse(initialController2.changeHandlerHistory.latestIsPush())
}

@Test
fun testSetBackStackForInvertHandlersWithoutRemovesView() {
val initialController1 = TestController()
val initialPushHandler1 = taggedHandler("initialPush1", true)
val initialPopHandler1 = taggedHandler("initialPop1", true)
val initialController1 = initialController1
.pushChangeHandler(initialPushHandler1)
.popChangeHandler(initialPopHandler1)
router.setRoot(initialController1)

val initialController2 = TestController()
val initialPushHandler2 = taggedHandler("initialPush2", false)
val initialPopHandler2 = taggedHandler("initialPop2", false)
val initialController2 = initialController2
.pushChangeHandler(initialPushHandler2)
.popChangeHandler(initialPopHandler2)
router.push(initialController2)

val initialView1 = initialController1.view
val initialView2 = initialController2.view

val setBackStackHandler = taggedHandler("setBackStackHandler", true)
val newBackStack = listOf(
initialController2,
initialController1
)

router.setBackStack(newBackStack, true, setBackStackHandler)

assertTrue(initialController1.changeHandlerHistory.isValidHistory)
assertTrue(initialController2.changeHandlerHistory.isValidHistory)

assertEquals(3, initialController1.changeHandlerHistory.size())
assertEquals(2, initialController2.changeHandlerHistory.size())

assertNotNull(initialController1.changeHandlerHistory.latestToView())
assertEquals(initialView2, initialController1.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController1.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController1.changeHandlerHistory.latestIsPush())

assertNotNull(initialController2.changeHandlerHistory.latestToView())
assertEquals(initialView2, initialController2.changeHandlerHistory.latestFromView())
assertEquals(
setBackStackHandler.tag,
initialController2.changeHandlerHistory.latestChangeHandler().tag
)
assertTrue(initialController2.changeHandlerHistory.latestIsPush())
}

/*
@Test
fun testSetBackStackCustomIsPush() {
val pushHandler1 = taggedHandler("pushHandler1", true)
val popHandler1 = taggedHandler("popHandler1", true)
val pushHandler2 = taggedHandler("pushHandler2", true)
val popHandler2 = taggedHandler("popHandler2", true)

val controller1 = TestController()
val controller1 = controller1.toController(pushHandler1, popHandler1)

val controller2 = TestController()
val controller2 = controller2.toController(pushHandler2, popHandler2)

var backStack = listOf(controller1, controller2)

router.setBackStack(backStack, true)

assertTrue(controller1.changeHandlerHistory.isValidHistory)
assertTrue(controller2.changeHandlerHistory.isValidHistory)

assertEquals(0, controller1.changeHandlerHistory.size())
assertEquals(1, controller2.changeHandlerHistory.size())

assertEquals(pushHandler2, controller2.changeHandlerHistory.latestChangeHandler())

backStack = listOf(controller2, controller1)
router.setBackStack(backStack, isPush = true)

assertTrue(controller1.changeHandlerHistory.isValidHistory)
assertTrue(controller2.changeHandlerHistory.isValidHistory)

assertEquals(1, controller1.changeHandlerHistory.size())
assertEquals(2, controller2.changeHandlerHistory.size())

assertEquals(pushHandler1, controller1.changeHandlerHistory.latestChangeHandler())
assertEquals(pushHandler1, controller2.changeHandlerHistory.latestChangeHandler())
}*/
}*/