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
    private val viewAttachHandler = ControllerAttachHandler(listener)

    @Test
    fun testActivityStartStop() {
        val view = View(activityProxy.activity)
        viewAttachHandler.listenForAttach(view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        viewAttachHandler.onActivityStarted()
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        viewAttachHandler.onActivityStopped()
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        viewAttachHandler.onActivityStarted()
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        viewAttachHandler.onActivityStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(0, listener.detachAfterStops)
    }

    @Test
    fun testSimpleViewAttachDetach() {
        viewAttachHandler.onActivityStarted()

        val view = View(activityProxy.activity)

        viewAttachHandler.listenForAttach(view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        viewAttachHandler.onActivityStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)

        viewAttachHandler.onActivityStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)
    }

    @Test
    fun testSimpleViewGroupAttachDetach() {
        viewAttachHandler.onActivityStarted()

        val view = LinearLayout(activityProxy.activity)
        viewAttachHandler.listenForAttach(view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        viewAttachHandler.onActivityStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)

        viewAttachHandler.onActivityStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)
    }

    @Test
    fun testNestedViewGroupAttachDetach() {
        viewAttachHandler.onActivityStarted()

        val view = LinearLayout(activityProxy.activity)
        val child = LinearLayout(activityProxy.activity)
        view.addView(child)
        viewAttachHandler.listenForAttach(view)

        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true, false)
        assertEquals(0, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(child, true, false)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true, false)
        ViewUtils.reportAttached(child, true, false)
        assertEquals(1, listener.attaches)
        assertEquals(0, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true, false)
        assertEquals(1, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(child, true, false)
        assertEquals(2, listener.attaches)
        assertEquals(1, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        viewAttachHandler.onActivityStopped()
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(0, listener.detachAfterStops)

        ViewUtils.reportAttached(view, false, false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)

        ViewUtils.reportAttached(view, true, false)
        ViewUtils.reportAttached(child, true, false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)

        viewAttachHandler.onActivityStarted()
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
        assertEquals(1, listener.detachAfterStops)
    }

    private class CountingControllerAttachHandlerListener : ControllerAttachHandler.Listener {

        var attaches = 0
        var detaches = 0
        var detachAfterStops = 0

        override fun onAttached() {
            attaches++
        }

        override fun onDetached(fromActivityStop: Boolean) {
            detaches++
        }

        override fun onViewDetachAfterStop() {
            detachAfterStops++
        }
    }
}