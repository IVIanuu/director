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
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.internal.DefaultControllerFactory
import com.ivianuu.director.util.ActivityProxy
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RouterTransactionTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.attachRouter(activityProxy.view)

    @Test
    fun testRouterSaveRestore() {
        val transaction = transaction {
            controller(TestController())
            tag("test")
            pushHandler(ChangeHandlerOne())
            popHandler(ChangeHandlerTwo())
        }

        // attach the controller because he needs the router for notifying lifecycle callbacks
        router.setRoot(transaction)

        val bundle = transaction.saveInstanceState()

        val restoredTransaction = RouterTransaction.fromBundle(bundle, DefaultControllerFactory())

        assertEquals(transaction.controller.javaClass, restoredTransaction.controller.javaClass)
        assertEquals(
            transaction.pushChangeHandler?.javaClass,
            restoredTransaction.pushChangeHandler?.javaClass
        )
        assertEquals(
            transaction.popChangeHandler?.javaClass,
            restoredTransaction.popChangeHandler?.javaClass
        )
        assertEquals(transaction.tag, restoredTransaction.tag)
    }
}

class ChangeHandlerOne : ControllerChangeHandler() {
    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
    }
}

class ChangeHandlerTwo : ControllerChangeHandler() {
    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    ) {
    }
}