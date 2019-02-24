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

import android.os.Bundle
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.CallState
import com.ivianuu.director.util.MockChangeHandler
import com.ivianuu.director.util.TestController
import com.ivianuu.director.util.defaultHandler
import com.ivianuu.director.util.listeningChangeHandler
import com.ivianuu.director.util.reportAttached
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
class ControllerLifecycleCallbacksTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1).apply {
        if (!hasRoot) {
            setRoot(TestController().toTransaction())
        }
    }

    private val currentCallState = CallState()

    @Test
    fun testNormalLifecycle() {
        val controller = TestController()
        attachLifecycleListener(controller)

        val expectedCallState = CallState()
        assertCalls(expectedCallState, controller)

        router.push(
            controller.toTransaction()
                .pushChangeHandler(getPushHandler(expectedCallState, controller))
                .popChangeHandler(getPopHandler(expectedCallState, controller))
        )

        assertCalls(expectedCallState, controller)

        router.popTop()

        assertNull(controller.view)

        assertCalls(expectedCallState, controller)
    }

    @Test
    fun testLifecycleWithActivityStop() {
        val controller = TestController()
        attachLifecycleListener(controller)

        val expectedCallState = CallState()

        assertCalls(expectedCallState, controller)
        router.push(
            controller.toTransaction()
                .pushChangeHandler(getPushHandler(expectedCallState, controller))
        )

        assertCalls(expectedCallState, controller)

        activityProxy.pause()

        assertCalls(expectedCallState, controller)

        activityProxy.stop()

        expectedCallState.detachCalls++
        assertCalls(expectedCallState, controller)

        assertNotNull(controller.view)
        controller.view!!.reportAttached(false)

        expectedCallState.saveViewStateCalls++
        expectedCallState.unbindViewCalls++
        assertCalls(expectedCallState, controller)
    }

    @Test
    fun testLifecycleWithActivityDestroy() {
        val controller = TestController()
        attachLifecycleListener(controller)

        val expectedCallState = CallState()

        assertCalls(expectedCallState, controller)
        router.push(
            controller.toTransaction()
                .pushChangeHandler(getPushHandler(expectedCallState, controller))
        )

        assertCalls(expectedCallState, controller)

        activityProxy.pause()

        assertCalls(expectedCallState, controller)

        activityProxy.stop()

        expectedCallState.detachCalls++
        assertCalls(expectedCallState, controller)

        activityProxy.destroy()

        expectedCallState.unbindViewCalls++
        expectedCallState.destroyCalls++
        assertCalls(expectedCallState, controller)
    }

    @Test
    fun testLifecycleWithActivityBackground() {
        val controller = TestController()
        attachLifecycleListener(controller)

        val expectedCallState = CallState()

        assertCalls(expectedCallState, controller)
        router.push(
            controller.toTransaction()
                .pushChangeHandler(getPushHandler(expectedCallState, controller))
        )

        assertCalls(expectedCallState, controller)

        activityProxy.pause()

        val bundle = Bundle()
        activityProxy.saveInstanceState(bundle)

        expectedCallState.saveInstanceStateCalls++
        expectedCallState.saveViewStateCalls++
        assertCalls(expectedCallState, controller)

        activityProxy.resume()

        assertCalls(expectedCallState, controller)
    }

    @Test
    fun testLifecycleCallOrder() {
        val testController = TestController()
        val callState = CallState()

        testController.addLifecycleListener(object : ControllerLifecycleListener {

            override fun preCreate(controller: Controller, savedInstanceState: Bundle?) {
                callState.createCalls++
                assertEquals(1, callState.createCalls)
                assertEquals(0, testController.currentCallState.createCalls)

                assertEquals(0, callState.buildViewCalls)
                assertEquals(0, testController.currentCallState.buildViewCalls)

                assertEquals(0, callState.bindViewCalls)
                assertEquals(0, testController.currentCallState.bindViewCalls)

                assertEquals(0, callState.attachCalls)
                assertEquals(0, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
                callState.createCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(0, callState.buildViewCalls)
                assertEquals(0, testController.currentCallState.buildViewCalls)

                assertEquals(0, callState.bindViewCalls)
                assertEquals(0, testController.currentCallState.bindViewCalls)

                assertEquals(0, callState.attachCalls)
                assertEquals(0, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun preBuildView(controller: Controller, savedViewState: Bundle?) {
                callState.buildViewCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(1, callState.buildViewCalls)
                assertEquals(0, testController.currentCallState.buildViewCalls)

                assertEquals(0, callState.bindViewCalls)
                assertEquals(0, testController.currentCallState.bindViewCalls)

                assertEquals(0, callState.attachCalls)
                assertEquals(0, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun postBuildView(
                controller: Controller,
                view: View,
                savedViewState: Bundle?
            ) {
                callState.buildViewCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(0, callState.bindViewCalls)
                assertEquals(0, testController.currentCallState.bindViewCalls)

                assertEquals(0, callState.attachCalls)
                assertEquals(0, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
                callState.bindViewCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(1, callState.bindViewCalls)
                assertEquals(0, testController.currentCallState.bindViewCalls)

                assertEquals(0, callState.attachCalls)
                assertEquals(0, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
                callState.bindViewCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(0, callState.attachCalls)
                assertEquals(0, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun preAttach(controller: Controller, view: View) {
                callState.attachCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(1, callState.attachCalls)
                assertEquals(0, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun postAttach(controller: Controller, view: View) {
                callState.attachCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(2, callState.attachCalls)
                assertEquals(1, testController.currentCallState.attachCalls)

                assertEquals(0, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun preDetach(controller: Controller, view: View) {
                callState.detachCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(2, callState.attachCalls)
                assertEquals(1, testController.currentCallState.attachCalls)

                assertEquals(1, callState.detachCalls)
                assertEquals(0, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun postDetach(controller: Controller, view: View) {
                callState.detachCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(2, callState.attachCalls)
                assertEquals(1, testController.currentCallState.attachCalls)

                assertEquals(2, callState.detachCalls)
                assertEquals(1, testController.currentCallState.detachCalls)

                assertEquals(0, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun preUnbindView(controller: Controller, view: View) {
                callState.unbindViewCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(2, callState.attachCalls)
                assertEquals(1, testController.currentCallState.attachCalls)

                assertEquals(2, callState.detachCalls)
                assertEquals(1, testController.currentCallState.detachCalls)

                assertEquals(1, callState.unbindViewCalls)
                assertEquals(0, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun postUnbindView(controller: Controller) {
                callState.unbindViewCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(2, callState.attachCalls)
                assertEquals(1, testController.currentCallState.attachCalls)

                assertEquals(2, callState.detachCalls)
                assertEquals(1, testController.currentCallState.detachCalls)

                assertEquals(2, callState.unbindViewCalls)
                assertEquals(1, testController.currentCallState.unbindViewCalls)

                assertEquals(0, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun preDestroy(controller: Controller) {
                callState.destroyCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(2, callState.attachCalls)
                assertEquals(1, testController.currentCallState.attachCalls)

                assertEquals(2, callState.detachCalls)
                assertEquals(1, testController.currentCallState.detachCalls)

                assertEquals(2, callState.unbindViewCalls)
                assertEquals(1, testController.currentCallState.unbindViewCalls)

                assertEquals(1, callState.destroyCalls)
                assertEquals(0, testController.currentCallState.destroyCalls)
            }

            override fun postDestroy(controller: Controller) {
                callState.destroyCalls++
                assertEquals(2, callState.createCalls)
                assertEquals(1, testController.currentCallState.createCalls)

                assertEquals(2, callState.buildViewCalls)
                assertEquals(1, testController.currentCallState.buildViewCalls)

                assertEquals(2, callState.bindViewCalls)
                assertEquals(1, testController.currentCallState.bindViewCalls)

                assertEquals(2, callState.attachCalls)
                assertEquals(1, testController.currentCallState.attachCalls)

                assertEquals(2, callState.detachCalls)
                assertEquals(1, testController.currentCallState.detachCalls)

                assertEquals(2, callState.unbindViewCalls)
                assertEquals(1, testController.currentCallState.unbindViewCalls)

                assertEquals(2, callState.destroyCalls)
                assertEquals(1, testController.currentCallState.destroyCalls)
            }
        })

        router.push(
            testController.toTransaction()
                .changeHandler(defaultHandler())
        )

        router.pop(testController)

        assertEquals(2, callState.createCalls)
        assertEquals(2, callState.buildViewCalls)
        assertEquals(2, callState.bindViewCalls)
        assertEquals(2, callState.attachCalls)
        assertEquals(2, callState.detachCalls)
        assertEquals(2, callState.unbindViewCalls)
        assertEquals(2, callState.destroyCalls)
    }

    @Test
    fun testChildLifecycle1() {
        val parent = TestController()
        router.push(
            parent.toTransaction()
                .changeHandler(defaultHandler())
        )

        val child = TestController()
        attachLifecycleListener(child)

        val expectedCallState = CallState()

        assertCalls(expectedCallState, child)

        val childRouter =
            parent.getChildRouter(parent.childContainer1!!).apply {
                setRoot(
                    child.toTransaction()
                        .pushChangeHandler(getPushHandler(expectedCallState, child))
                        .popChangeHandler(getPopHandler(expectedCallState, child))
                )
            }

        assertCalls(expectedCallState, child)

        parent.removeChildRouter(childRouter)

        assertCalls(expectedCallState, child)
    }

    @Test
    fun testChildLifecycle2() {
        val parent = TestController()
        router.push(
            parent.toTransaction()
                .changeHandler(defaultHandler())
        )

        val child = TestController()
        attachLifecycleListener(child)

        val expectedCallState = CallState()

        assertCalls(expectedCallState, child)

        parent.getChildRouter(parent.childContainer1!!).apply {
            setRoot(
                child.toTransaction()
                    .pushChangeHandler(getPushHandler(expectedCallState, child))
                    .popChangeHandler(getPopHandler(expectedCallState, child))
            )
        }

        assertCalls(expectedCallState, child)

        router.popTop()

        expectedCallState.detachCalls++
        expectedCallState.unbindViewCalls++
        expectedCallState.destroyCalls++

        assertCalls(expectedCallState, child)
    }

    @Test
    fun testChildLifecycleOrderingAfterUnexpectedAttach() {
        val parent = TestController()
        parent.retainView = true
        router.push(
            parent.toTransaction()
                .changeHandler(defaultHandler())
        )

        val child = TestController()
        child.retainView = true
        parent.getChildRouter(parent.childContainer1!!).apply {
            setRoot(
                child.toTransaction()
                    .changeHandler(SimpleSwapChangeHandler())
            )
        }

        assertTrue(parent.isAttached)
        assertTrue(child.isAttached)

        parent.view!!.reportAttached(attached = false, applyOnChildren = true)
        assertFalse(parent.isAttached)
        assertFalse(child.isAttached)

        child.view!!.reportAttached(true)
        assertFalse(parent.isAttached)
        assertFalse(child.isAttached)

        parent.view!!.reportAttached(true)
        assertTrue(parent.isAttached)
        assertTrue(child.isAttached)
    }

    private fun getPushHandler(
        expectedCallState: CallState,
        controller: TestController
    ): MockChangeHandler {
        return listeningChangeHandler(object : MockChangeHandler.Listener {
            override fun willStartChange() {
                expectedCallState.createCalls++
                expectedCallState.changeStartCalls++
                expectedCallState.buildViewCalls++
                expectedCallState.bindViewCalls++
                assertCalls(expectedCallState, controller)
            }

            override fun didAttachOrDetach() {
                expectedCallState.attachCalls++
                assertCalls(expectedCallState, controller)
            }

            override fun didEndChange() {
                expectedCallState.changeEndCalls++
                assertCalls(expectedCallState, controller)
            }
        })
    }

    private fun getPopHandler(
        expectedCallState: CallState,
        controller: TestController
    ): MockChangeHandler {
        return listeningChangeHandler(object : MockChangeHandler.Listener {
            override fun willStartChange() {
                expectedCallState.changeStartCalls++
                assertCalls(expectedCallState, controller)
            }

            override fun didAttachOrDetach() {
                expectedCallState.unbindViewCalls++
                expectedCallState.detachCalls++
                expectedCallState.destroyCalls++
                assertCalls(expectedCallState, controller)
            }

            override fun didEndChange() {
                expectedCallState.changeEndCalls++
                assertCalls(expectedCallState, controller)
            }
        })
    }

    private fun assertCalls(callState: CallState, controller: TestController) {
        assertEquals(
            "Expected call counts and controller call counts do not match.",
            callState,
            controller.currentCallState
        )
        assertEquals(
            "Expected call counts and lifecycle call counts do not match.",
            callState,
            currentCallState
        )
    }

    private fun attachLifecycleListener(controller: Controller) {
        controller.addLifecycleListener(object : ControllerLifecycleListener {
            override fun onChangeStart(
                controller: Controller,
                changeHandler: ControllerChangeHandler,
                changeType: ControllerChangeType
            ) {
                currentCallState.changeStartCalls++
            }

            override fun onChangeEnd(
                controller: Controller,
                changeHandler: ControllerChangeHandler,
                changeType: ControllerChangeType
            ) {
                currentCallState.changeEndCalls++
            }

            override fun postCreate(controller: Controller, savedInstanceState: Bundle?) {
                currentCallState.createCalls++
            }

            override fun postBuildView(
                controller: Controller,
                view: View,
                savedViewState: Bundle?
            ) {
                currentCallState.buildViewCalls++
            }

            override fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
                currentCallState.bindViewCalls++
            }

            override fun postAttach(controller: Controller, view: View) {
                currentCallState.attachCalls++
            }

            override fun postUnbindView(controller: Controller) {
                super.postUnbindView(controller)
                currentCallState.unbindViewCalls++
            }

            override fun postDetach(controller: Controller, view: View) {
                currentCallState.detachCalls++
            }

            override fun postDestroy(controller: Controller) {
                currentCallState.destroyCalls++
            }

            override fun onRestoreInstanceState(
                controller: Controller,
                savedInstanceState: Bundle
            ) {
                super.onRestoreInstanceState(controller, savedInstanceState)
                currentCallState.restoreInstanceStateCalls++
            }

            override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
                currentCallState.saveInstanceStateCalls++
            }

            override fun onRestoreViewState(
                controller: Controller,
                view: View,
                savedViewState: Bundle
            ) {
                super.onRestoreViewState(controller, view, savedViewState)
                currentCallState.restoreViewStateCalls++
            }

            override fun onSaveViewState(controller: Controller, view: View, outState: Bundle) {
                currentCallState.saveViewStateCalls++
            }

        })
    }

    @Test
    fun testLastInFirstOutRule() {
        val parent = TestController()
        val child = TestController()

        val listener = LastInFirstOutListener(parent, child)

        parent.addLifecycleListener(listener)
        child.addLifecycleListener(listener)

        parent.addLifecycleListener(
            preCreate = { _, _ ->
                parent.getChildRouter(TestController.CHILD_VIEW_ID_1)
                    .push(child.toTransaction())
            },
            postAttach = { _, _ -> router.popTop() }
        )

        router.push(parent.toTransaction())
    }

    private class LastInFirstOutListener(
        private val parent: Controller,
        private val child: Controller
    ) : ControllerLifecycleListener {

        override fun preBindView(controller: Controller, view: View, savedViewState: Bundle?) {
            super.preBindView(controller, view, savedViewState)
            when (controller) {
                parent -> assertNull(child.view)
                child -> assertNotNull(parent.view)
            }
        }

        override fun postBindView(controller: Controller, view: View, savedViewState: Bundle?) {
            super.postBindView(controller, view, savedViewState)
            when (controller) {
                parent -> assertNull(child.view)
                child -> assertNotNull(parent.view)
            }
        }

        override fun preAttach(controller: Controller, view: View) {
            super.preAttach(controller, view)
            when (controller) {
                parent -> assertFalse(child.isAttached)
                child -> assertTrue(parent.isAttached)
            }
        }

        override fun postAttach(controller: Controller, view: View) {
            super.postAttach(controller, view)
            when (controller) {
                parent -> assertFalse(child.isAttached)
                child -> assertTrue(parent.isAttached)
            }
        }

        override fun preDetach(controller: Controller, view: View) {
            super.preDetach(controller, view)
            when (controller) {
                parent -> assertFalse(child.isAttached)
                child -> assertTrue(parent.isAttached)
            }
        }

        override fun postDetach(controller: Controller, view: View) {
            super.postDetach(controller, view)
            when (controller) {
                parent -> assertFalse(child.isAttached)
                child -> assertTrue(parent.isAttached)
            }
        }

        override fun preUnbindView(controller: Controller, view: View) {
            super.preUnbindView(controller, view)
            when (controller) {
                parent -> assertNull(child.view)
                child -> assertNotNull(parent.view)
            }
        }

        override fun postUnbindView(controller: Controller) {
            super.postUnbindView(controller)
            when (controller) {
                parent -> assertNull(child.view)
                child -> assertNotNull(parent.view)
            }
        }

        override fun preDestroy(controller: Controller) {
            super.preDestroy(controller)
            when (controller) {
                parent -> assertTrue(child.isDestroyed)
                child -> assertFalse(parent.isDestroyed)
            }
        }

        override fun postDestroy(controller: Controller) {
            super.postDestroy(controller)
            when (controller) {
                parent -> assertTrue(child.isDestroyed)
                child -> assertFalse(parent.isDestroyed)
            }
        }

    }
}