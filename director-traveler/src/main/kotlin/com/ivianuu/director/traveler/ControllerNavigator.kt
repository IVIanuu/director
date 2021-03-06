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

package com.ivianuu.director.traveler

import com.ivianuu.director.Controller
import com.ivianuu.director.Router
import com.ivianuu.director.RouterTransaction
import com.ivianuu.director.findControllerByTag
import com.ivianuu.director.hasRoot
import com.ivianuu.director.popController
import com.ivianuu.director.popToRoot
import com.ivianuu.director.popTop
import com.ivianuu.director.push
import com.ivianuu.director.replaceTop
import com.ivianuu.director.toTransaction
import com.ivianuu.traveler.Back
import com.ivianuu.traveler.BackTo
import com.ivianuu.traveler.Command
import com.ivianuu.traveler.Forward
import com.ivianuu.traveler.Navigator
import com.ivianuu.traveler.Replace
import com.ivianuu.traveler.common.ResultNavigator

/**
 * A [Navigator] for [Controller]s
 */
open class ControllerNavigator(private val router: Router) : ResultNavigator() {

    override fun applyCommandWithResult(command: Command): Boolean = when (command) {
        is Forward -> forward(command)
        is Replace -> replace(command)
        is Back -> back(command)
        is BackTo -> backTo(command)
        else -> unsupportedCommand(command)
    }

    protected open fun forward(command: Forward): Boolean {
        val controller = createController(command.key, command.data)
            ?: return unknownScreen(command.key)

        val transaction = controller.toTransaction()

        val currentController = router.backStack.lastOrNull()?.controller

        setupTransaction(command, currentController, controller, transaction)
        transaction.tag = getControllerTag(command.key)

        router.push(transaction)

        return true
    }

    protected open fun replace(command: Replace): Boolean {
        val controller = createController(command.key, command.data)
            ?: return unknownScreen(command.key)

        val transaction = controller.toTransaction()

        val currentController = router.backStack.lastOrNull()?.controller

        setupTransaction(command, currentController, controller, transaction)
        transaction.tag = getControllerTag(command.key)

        router.replaceTop(transaction)

        return true
    }

    protected open fun back(command: Back): Boolean = if (router.hasRoot
        && (router.popsLastView || router.backStack.size > 1)
    ) {
        router.popTop()
        true
    } else {
        exit()
    }

    protected open fun backTo(command: BackTo): Boolean {
        val key = command.key

        return if (key != null) {
            val controller = router.findControllerByTag(getControllerTag(key))
            if (controller != null) {
                router.popController(controller)
                true
            } else {
                backToUnexisting(key)
            }
        } else {
            backToRoot()
        }
    }

    protected open fun backToRoot(): Boolean {
        router.popToRoot()
        return true
    }

    protected open fun backToUnexisting(key: Any): Boolean {
        router.popToRoot()
        return true
    }

    /**
     * Will be called when the backStack is empty and the hosting activity should be closed
     * This is a no op by default
     */
    protected open fun exit(): Boolean = true

    /**
     * Creates the corresponding [Controller] for [key]
     */
    protected open fun createController(key: Any, data: Any?): Controller? = when (key) {
        is ControllerKey -> key.createController(data)
        else -> null
    }

    /**
     * Returns the corresponding controller tag for [key]
     */
    protected open fun getControllerTag(key: Any): String = when (key) {
        is ControllerKey -> key.getControllerTag()
        else -> key.toString()
    }

    /**
     * Add change handlers etc
     */
    protected open fun setupTransaction(
        command: Command,
        currentController: Controller?,
        nextController: Controller,
        transaction: RouterTransaction
    ) {
        val key = when (command) {
            is Forward -> command.key
            is Replace -> command.key
            else -> null
        } as? ControllerKey ?: return

        return key.setupTransaction(command, currentController, nextController, transaction)
    }

    /**
     * Will be called when a unknown screen was requested
     */
    protected open fun unknownScreen(key: Any): Boolean = false

    /**
     * Will be called when a unsupported command was send
     */
    protected open fun unsupportedCommand(command: Command): Boolean = false
}