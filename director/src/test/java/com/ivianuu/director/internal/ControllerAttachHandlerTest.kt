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
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.ViewUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(AndroidJUnit4::class)
class ControllerAttachHandlerTest {

    private val activityProxy = ActivityProxy()
    private val listener = CountingControllerAttachHandlerListener()
    private val viewAttachHandler = ControllerAttachHandler(true, listener)

    @Test
    fun testActivityStartStop() {
        val view = View(activityProxy.activity)
        viewAttachHandler.takeView(view)
        viewAttachHandler.parentAttached()

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, true)
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
    fun testParentAttachDetach() {
        val view = View(activityProxy.activity)
        viewAttachHandler.takeView(view)
        viewAttachHandler.hostStarted()
        ViewUtils.reportAttached(view, true)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        viewAttachHandler.parentAttached()
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        viewAttachHandler.parentDetached()
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.parentAttached()
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.parentDetached()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testSimpleViewAttachDetach() {
        viewAttachHandler.hostStarted()
        viewAttachHandler.parentAttached()

        val view = View(activityProxy.activity)

        viewAttachHandler.takeView(view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        ViewUtils.reportAttached(view, false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testSimpleViewGroupAttachDetach() {
        viewAttachHandler.hostStarted()
        viewAttachHandler.parentAttached()

        val view = LinearLayout(activityProxy.activity)
        viewAttachHandler.takeView(view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        ViewUtils.reportAttached(view, false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testNestedViewGroupAttachDetach() {
        viewAttachHandler.hostStarted()
        viewAttachHandler.parentAttached()

        val view = LinearLayout(activityProxy.activity)
        val child = LinearLayout(activityProxy.activity)
        view.addView(child)
        viewAttachHandler.takeView(view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, attached = true, propogateToChildren = false)
        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(child, attached = true, propogateToChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, attached = true, propogateToChildren = false)
        ViewUtils.reportAttached(child, attached = true, propogateToChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)

        ViewUtils.reportAttached(view, attached = false, propogateToChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        ViewUtils.reportAttached(view, attached = false, propogateToChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        ViewUtils.reportAttached(view, attached = true, propogateToChildren = false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)

        ViewUtils.reportAttached(child, attached = true, propogateToChildren = false)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)

        viewAttachHandler.hostStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        ViewUtils.reportAttached(view, attached = false, propogateToChildren = false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        ViewUtils.reportAttached(view, attached = true, propogateToChildren = false)
        ViewUtils.reportAttached(child, attached = true, propogateToChildren = false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        viewAttachHandler.hostStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    private class CountingControllerAttachHandlerListener :
            (ControllerAttachHandler.ChangeReason, Boolean, Boolean, Boolean) -> Unit {

        var attaches = 0
        var detaches = 0

        private var wasAttached = false

        override fun invoke(
            reason: ControllerAttachHandler.ChangeReason,
            viewAttached: Boolean,
            parentAttached: Boolean,
            hostStarted: Boolean
        ) {
            val isAttached = viewAttached && parentAttached && hostStarted
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