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

package com.ivianuu.director.testing

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.ControllerState
import com.ivianuu.director.ControllerState.ATTACHED
import com.ivianuu.director.ControllerState.CREATED
import com.ivianuu.director.ControllerState.DESTROYED
import com.ivianuu.director.ControllerState.INITIALIZED
import com.ivianuu.director.ControllerState.VIEW_BOUND
import com.ivianuu.director.Router
import com.ivianuu.director.fragmenthost.getRouter
import com.ivianuu.director.pop
import com.ivianuu.director.setRoot
import kotlin.reflect.KClass

class ControllerScenario<C : Controller> internal constructor(
    private val controllerClass: KClass<C>,
    private val args: Bundle,
    private val controllerFactory: ControllerFactory
) {

    private val activityScenario = ActivityScenario.launch(EmptyControllerActivity::class.java)

    init {
        activityScenario.onActivity { activity ->
            val viewModelProvider = ViewModelProvider(
                activity,
                ViewModelProvider.AndroidViewModelFactory.getInstance(
                    activity.application
                )
            )
            viewModelProvider
                .get(ControllerFactoryHolderViewModel::class.java)
                .controllerFactory = controllerFactory
            activity.router.controllerFactory = controllerFactory

            val controller = controllerFactory.createController(
                controllerClass.java.classLoader!!,
                controllerClass.java.name,
                args
            )

            activity.router.setRoot(controller, tag = CONTROLLER_TAG)
        }
    }

    fun moveToState(newState: ControllerState): ControllerScenario<C> {
        if (newState == DESTROYED) {
            activityScenario.onActivity { activity ->
                val controller = activity.router.findControllerByTag(CONTROLLER_TAG)
                if (controller != null) {
                    activity.router.pop(controller)
                }
            }
        } else {
            activityScenario.onActivity { activity ->
                activityScenario.moveToState(newState.toLifecycleState())
                activity.router.findControllerByTag(CONTROLLER_TAG)
                    ?: error("The controller already has been popped from the router.")

            }
        }
        return this
    }

    fun recreate(): ControllerScenario<C> {
        activityScenario.recreate()
        return this
    }

    fun onController(block: (C) -> Unit): ControllerScenario<C> {
        activityScenario.onActivity { activity ->
            val controller = activity.router.findControllerByTag(CONTROLLER_TAG)
                ?: error("The controller already has been popped from the router.")
            block(controller as C)
        }
        return this
    }

    private fun ControllerState.toLifecycleState(): Lifecycle.State = when (this) {
        INITIALIZED -> Lifecycle.State.INITIALIZED
        CREATED -> Lifecycle.State.CREATED
        VIEW_BOUND -> Lifecycle.State.STARTED
        ATTACHED -> Lifecycle.State.RESUMED
        DESTROYED -> Lifecycle.State.DESTROYED
    }

    companion object {
        private const val CONTROLLER_TAG = "ControllerScenario.controllerTag"

        internal class DefaultControllerFactory : ControllerFactory
    }
}

class EmptyControllerActivity : FragmentActivity() {

    val router: Router get() = _router
    private lateinit var _router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Checks if we have a custom controller factory and set it.
        val viewModelProvider = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )
        val factory = viewModelProvider
            .get(ControllerFactoryHolderViewModel::class.java)
            .controllerFactory

        _router = getRouter(android.R.id.content, factory)
    }
}

class ControllerFactoryHolderViewModel : ViewModel() {

    var controllerFactory: ControllerFactory? = null

    override fun onCleared() {
        super.onCleared()
        controllerFactory = null
    }
}

@PublishedApi
internal class SingleControllerFactory<C : Controller>(
    private val instantiate: (ClassLoader, String, Bundle) -> Controller
) : ControllerFactory {
    override fun createController(
        classLoader: ClassLoader,
        className: String,
        args: Bundle
    ): Controller = instantiate(classLoader, className, args)
}

inline fun <reified C : Controller> launch(
    args: Bundle? = null,
    noinline instantiate: (ClassLoader, String, Bundle) -> C
): ControllerScenario<C> = launch(C::class, args, instantiate)

fun <C : Controller> launch(
    controllerClass: KClass<C>,
    args: Bundle? = null,
    instantiate: (ClassLoader, String, Bundle) -> C
): ControllerScenario<C> = launch(controllerClass, args, SingleControllerFactory<C>(instantiate))

inline fun <reified C : Controller> launch(
    args: Bundle? = null,
    factory: ControllerFactory? = null
): ControllerScenario<C> = launch(C::class, args, factory)

fun <C : Controller> launch(
    controllerClass: KClass<C>,
    args: Bundle? = null,
    factory: ControllerFactory? = null
): ControllerScenario<C> = ControllerScenario(
    controllerClass,
    args ?: Bundle(),
    factory
        ?: ControllerScenario.Companion.DefaultControllerFactory()
)