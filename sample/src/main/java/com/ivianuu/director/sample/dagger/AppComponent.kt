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

package com.ivianuu.director.sample.dagger

import com.ivianuu.contributor.ContributeInjector
import com.ivianuu.director.contributor.DirectorInjectionModule
import com.ivianuu.director.sample.App
import com.ivianuu.director.sample.MainActivity
import com.ivianuu.director.sample.controller.DaggerController
import dagger.Component
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import javax.inject.Scope
import javax.inject.Singleton

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        ActivityBindingModule::class,
        DirectorInjectionModule::class
    ]
)
interface AppComponent : AndroidInjector<App> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<App>()
}

@Scope
annotation class PerActivity

@Scope
annotation class PerController

@Module
abstract class ActivityBindingModule {

    @PerActivity
    @ContributesAndroidInjector(modules = [ControllerBindingModule_Contributions::class])
    abstract fun bindMainActivity(): MainActivity
}

@Module
abstract class ControllerBindingModule {

    @PerController
    @ContributeInjector
    abstract fun bindDaggerController(): DaggerController
}