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

package com.ivianuu.director.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.testing.ControllerState
import com.ivianuu.director.testing.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SampleTest {

    @Test
    fun test() {
        val controllerScenario = launch<HomeController>()
        controllerScenario.moveToState(ControllerState.ATTACHED)
        controllerScenario.onController {
            // dump test
            assertEquals(it.viewModelStore, it.retainedObjects.entries.values.first())
        }
    }
}