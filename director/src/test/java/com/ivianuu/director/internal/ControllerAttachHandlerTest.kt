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

package com.ivianuu.director.internal

import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.AttachFakingFrameLayout
import com.ivianuu.director.util.reportAttached
import com.ivianuu.director.util.setParent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(AndroidJUnit4::class)
class ControllerAttachHandlerTest {

    private val activityProxy = ActivityProxy()
    private val listener = CountingControllerAttachHandlerListener()
    private val viewAttachHandler = ControllerAttachHandler(listener)

    @Test
    fun testActivityStartStop() {
        val container = AttachFakingFrameLayout(activityProxy.activity)
        val view = View(activityProxy.activity)
        view.setParent(container)

        viewAttachHandler.takeView(container, view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(true)
        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testSimpleViewAttachDetach() {
        viewAttachHandler.hostStarted()

        val container = AttachFakingFrameLayout(activityProxy.activity)
        val view = View(activityProxy.activity)
        view.setParent(container)

        viewAttachHandler.takeView(container, view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.reportAttached(false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.reportAttached(true)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(true)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testSimpleViewGroupAttachDetach() {
        viewAttachHandler.hostStarted()

        val container = AttachFakingFrameLayout(activityProxy.activity)
        val view = View(activityProxy.activity)
        view.setParent(container)

        viewAttachHandler.takeView(container, view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.reportAttached(false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.reportAttached(true)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(true)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testNestedViewGroupAttachDetach() {
        viewAttachHandler.hostStarted()

        val container = AttachFakingFrameLayout(activityProxy.activity)
        val view = FrameLayout(activityProxy.activity)
        view.setParent(container)

        val child = FrameLayout(activityProxy.activity)
        view.addView(child)

        viewAttachHandler.takeView(container, view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(attached = true, applyOnChildren = false)
        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        child.reportAttached(attached = true, applyOnChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(attached = true, applyOnChildren = false)
        child.reportAttached(attached = true, applyOnChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(attached = false, applyOnChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.reportAttached(attached = false, applyOnChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.reportAttached(attached = true, applyOnChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        child.reportAttached(attached = true, applyOnChildren = false)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(attached = false, applyOnChildren = false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(attached = true, applyOnChildren = false)
        child.reportAttached(attached = true, applyOnChildren = false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testAttachedToUnownedParent() {
        viewAttachHandler.hostStarted()

        val container = AttachFakingFrameLayout(activityProxy.activity)
        val otherContainer = AttachFakingFrameLayout(activityProxy.activity)
        val view = FrameLayout(activityProxy.activity)

        viewAttachHandler.takeView(container, view)

        view.setParent(otherContainer)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(true)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(false)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        view.setParent(container)

        view.reportAttached(true)

        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        view.reportAttached(false)

        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.setParent(otherContainer)

        view.reportAttached(true)

        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        view.reportAttached(false)

        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
    }

    private class CountingControllerAttachHandlerListener :
            (ControllerAttachHandler.ChangeReason, Boolean, Boolean) -> Unit {

        var attaches = 0
        var detaches = 0

        private var wasAttached = false

        override fun invoke(
            reason: ControllerAttachHandler.ChangeReason,
            viewAttached: Boolean,
            hostStarted: Boolean
        ) {
            val isAttached = viewAttached && hostStarted
            if (isAttached != wasAttached) {
                wasAttached = isAttached
                if (isAttached) {
                    attaches++
                } else {
                    detaches++
                }
            }
        }

    }
}