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
import com.ivianuu.director.Controller
import com.ivianuu.director.Router
import com.ivianuu.director.hasRoot

class SingleContainer(val router: Router) {

    val isEmpty get() = router.hasRoot

    val currentController: Controller? get() = router.backstack.lastOrNull()
    val detachedControllers: List<Controller>? get() = router.backstack.dropLast(1)

    fun set(controller: Controller) {
        if (controller == currentController) return

        controller.pushChangeHandler.check()
        controller.popChangeHandler.check()

        val newBackstack = router.backstack.toMutableList()
        val index = newBackstack.indexOf(controller)

        if (index != -1) newBackstack.removeAt(index)
        newBackstack.add(controller)

        router.setBackstack(newBackstack, isPush = true)
    }

    fun remove(controller: Controller) {
        val newBackstack = router.backstack.toMutableList()
        newBackstack.remove(controller)
        router.setBackstack(newBackstack, false)
    }

    private fun ControllerChangeHandler?.check() {
        check(this?.removesFromViewOnPush ?: true) {
            "Must remove from view while using single container"
        }
    }
}

inline fun Router.moveToTop(tag: String, create: () -> Controller) {
    val backstack = backstack.toMutableList()
    var controller = backstack.firstOrNull { it.tag == tag }
    if (controller != null) {
        backstack.remove(controller)
    } else {
        controller = create()
    }

    backstack.add(controller)
    setBackstack(backstack, true)
}

inline fun SingleContainer.setIfEmpty(create: () -> Controller) {
    if (isEmpty) set(create())
}

inline fun SingleContainer.setByTag(tag: String, create: () -> Controller) {
    set(router.backstack.firstOrNull { it.tag == tag } ?: create())
}

fun SingleContainer.removeByTag(tag: String) {
    router.backstack.firstOrNull { it.tag == tag }?.let { remove(it) }
}