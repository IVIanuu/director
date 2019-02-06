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
import com.ivianuu.director.util.reportAttached
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(AndroidJUnit4::class)
class ViewAttachHandlerTest {

    private val activityProxy = ActivityProxy()
    private val listener = CountingViewAttachHandlerListener()
    private val viewAttachHandler = ViewAttachHandler(listener)

    @Test
    fun testSimpleViewAttachDetach() {
        val view = View(activityProxy.activity)

        viewAttachHandler.takeView(view)

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

        view.reportAttached(false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(true)
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testSimpleViewGroupAttachDetach() {
        val view = View(activityProxy.activity)

        viewAttachHandler.takeView(view)

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

        view.reportAttached(false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(true)
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    @Test
    fun testNestedViewGroupAttachDetach() {
        val view = FrameLayout(activityProxy.activity)

        val child = FrameLayout(activityProxy.activity)
        view.addView(child)

        viewAttachHandler.takeView(view)

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

        view.reportAttached(attached = false, applyOnChildren = false)
        assertEquals(2, listener.attaches)
        assertEquals(2, listener.detaches)

        view.reportAttached(attached = true, applyOnChildren = false)
        child.reportAttached(attached = true, applyOnChildren = false)
        assertEquals(3, listener.attaches)
        assertEquals(2, listener.detaches)
    }

    private class CountingViewAttachHandlerListener : (Boolean) -> Unit {

        var attaches = 0
        var detaches = 0

        private var wasAttached = false

        override fun invoke(attached: Boolean) {
            if (attached != wasAttached) {
                wasAttached = attached
                if (attached) {
                    attaches++
                } else {
                    detaches++
                }
            }
        }

    }
}