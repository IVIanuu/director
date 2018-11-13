package com.ivianuu.director.contributor

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

import com.ivianuu.director.Controller
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.Multibinds

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface HasControllerInjector {
    fun controllerInjector(): AndroidInjector<Controller>
}

/**
 * @author Manuel Wrage (IVIanuu)
 */
object DirectorInjection {

    fun inject(controller: Controller) {
        val hasDispatchingControllerInjector = findHasControllerInjector(controller)
        val controllerInjector = hasDispatchingControllerInjector.controllerInjector()
        controllerInjector.inject(controller)
    }

    private fun findHasControllerInjector(controller: Controller): HasControllerInjector {
        if (controller.parentController != null) {
            var parent = controller.parentController

            while (parent != null) {
                if (parent is HasControllerInjector) {
                    return parent
                }

                parent = parent.parentController
            }
        }

        val activity = controller.activity
        if (activity is HasControllerInjector) {
            return activity
        }

        val application = activity.application
        if (application is HasControllerInjector) {
            return application
        }

        throw IllegalArgumentException("no injector found for ${controller.javaClass.name}")
    }
}

/**
 * Director injection module
 */
@Module
abstract class DirectorInjectionModule {

    @Multibinds
    abstract fun controllerInjectorFactories(): Map<Class<out Controller>, AndroidInjector.Factory<out Controller>>

    @Multibinds
    abstract fun controllerInjectorFactoriesWithStringKeys(): Map<String, AndroidInjector.Factory<out Controller>>
}
