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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.util.ActivityProxy
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ControllerStateTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1)

    @Test
    fun testSaveRestoreInstanceState() {
        var controller = StateController()
        controller.instanceStateValue = "my_state"

        router.push(controller)

        val savedState = router.saveInstanceState()
        router.restoreInstanceState(savedState)
        controller = router.backstack.first() as StateController

        assertEquals("my_state", controller.instanceStateValue)
    }

    @Test
    fun testSaveRestoreViewState() {
        var controller = StateController()
        controller.viewStateValue = "my_state"

        router.push(controller)

        val savedState = router.saveInstanceState()
        router.restoreInstanceState(savedState)
        controller = router.backstack.first() as StateController

        assertEquals("my_state", controller.viewStateValue)
    }

}

class StateController : Controller() {

    var instanceStateValue: String? = null
    var viewStateValue: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View = View(activity)

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        instanceStateValue = savedInstanceState.getString("value")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("value", instanceStateValue)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        viewStateValue = savedViewState.getString("value")
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        outState.putString("value", viewStateValue)
    }

}