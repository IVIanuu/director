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

package com.ivianuu.director.sample.util

import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.Router
import com.ivianuu.director.RouterTransaction
import com.ivianuu.director.hasRoot

class SingleContainer(val router: Router) {

    val isEmpty get() = router.hasRoot

    val currentTransaction: RouterTransaction? get() = router.backStack.lastOrNull()
    val detachedTransactions: List<RouterTransaction>? get() = router.backStack.dropLast(1)

    fun set(transaction: RouterTransaction) {
        if (transaction == currentTransaction) return

        transaction.pushChangeHandler.check()
        transaction.popChangeHandler.check()

        val newBackStack = router.backStack.toMutableList()
        val index = newBackStack.indexOf(transaction)

        if (index != -1) newBackStack.removeAt(index)
        newBackStack.add(transaction)

        router.setBackStack(newBackStack, isPush = true)
    }

    fun remove(transaction: RouterTransaction) {
        val newBackStack = router.backStack.toMutableList()
        newBackStack.remove(transaction)
        router.setBackStack(newBackStack, false)
    }

    private fun ControllerChangeHandler?.check() {
        check(this?.removesFromViewOnPush ?: true) {
            "Must remove from view while using single container"
        }
    }
}

inline fun Router.moveToTop(tag: String, create: () -> RouterTransaction) {
    val backStack = backStack.toMutableList()
    var controller = backStack.firstOrNull { it.tag == tag }
    if (controller != null) {
        backStack.remove(controller)
    } else {
        controller = create()
    }

    backStack.add(controller)
    setBackStack(backStack, true)
}

inline fun SingleContainer.setIfEmpty(create: () -> RouterTransaction) {
    if (isEmpty) set(create())
}

inline fun SingleContainer.setByTag(tag: String, create: () -> RouterTransaction) {
    set(router.backStack.firstOrNull { it.tag == tag } ?: create())
}

fun SingleContainer.removeByTag(tag: String) {
    router.backStack.firstOrNull { it.tag == tag }?.let { remove(it) }
}