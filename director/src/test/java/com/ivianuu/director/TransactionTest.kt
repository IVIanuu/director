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
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TransactionTest {

    private val activityProxy = ActivityProxy().create(null).start().resume()
    private val router = activityProxy.activity.getRouter(activityProxy.view1)

    @Test
    fun testRouterSaveRestore() {
        val transaction = Transaction(TestController())
            .pushChangeHandler(ChangeHandlerOne())
            .popChangeHandler(ChangeHandlerTwo())
            .tag("test")

        // attach the controller because he needs the router for notifying lifecycle callbacks
        router.setRoot(transaction)

        val bundle = transaction.saveInstanceState()

        val restoredTransaction =
            Transaction.fromBundle(bundle, router.routerManager.controllerFactory)

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

class ChangeHandlerOne : ChangeHandler() {
    override fun performChange(changeData: ChangeData) {
    }
}

class ChangeHandlerTwo : ChangeHandler() {
    override fun performChange(changeData: ChangeData) {
    }
}