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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.director.toTransaction
import com.ivianuu.director.util.TestController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(AndroidJUnit4::class)
class BackstackTest {

    private val backstack = Backstack()

    @Test
    fun testPush() {
        assertEquals(0, backstack.size)
        val transaction = TestController().toTransaction()
        backstack.push(transaction)
        assertEquals(1, backstack.size)
        assertEquals(transaction, backstack.entries.firstOrNull())
    }

    @Test
    fun testPop() {
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()
        backstack.push(transaction1)
        backstack.push(transaction2)
        assertEquals(2, backstack.size)
        assertEquals(transaction1, backstack.entries[0])
        assertEquals(transaction2, backstack.entries[1])
        backstack.pop()
        assertEquals(1, backstack.size)
        assertEquals(transaction1, backstack.entries[0])
        backstack.pop()
        assertEquals(0, backstack.size)
    }

    @Test
    fun testPeek() {
        val transaction1 = TestController().toTransaction()
        val transaction2 = TestController().toTransaction()

        backstack.push(transaction1)
        assertEquals(transaction1, backstack.peek())

        backstack.push(transaction2)
        assertEquals(transaction2, backstack.peek())

        backstack.pop()
        assertEquals(transaction1, backstack.peek())
    }
}