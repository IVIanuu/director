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

package com.ivianuu.director.scopes

import com.ivianuu.director.Controller
import com.ivianuu.director.common.ControllerEvent
import com.ivianuu.director.common.ControllerEvent.ATTACH
import com.ivianuu.director.common.ControllerEvent.BIND_VIEW
import com.ivianuu.director.common.ControllerEvent.CREATE
import com.ivianuu.director.common.ControllerEvent.DESTROY
import com.ivianuu.director.common.ControllerEvent.DETACH
import com.ivianuu.director.common.ControllerEvent.UNBIND_VIEW
import com.ivianuu.scopes.Scope
import com.ivianuu.scopes.cache.LifecycleScopesStore
import com.ivianuu.scopes.lifecycle.LifecycleScopes

private val lifecycleScopesStore =
    LifecycleScopesStore<Controller, ControllerEvent>(DESTROY) {
    LifecycleScopes(ControllerLifecycle(it))
}

val Controller.lifecycleScopes: LifecycleScopes<ControllerEvent>
    get() = lifecycleScopesStore.get(this)

fun Controller.scopeFor(event: ControllerEvent): Scope =
    lifecycleScopes.scopeFor(event)

val Controller.create: Scope get() = scopeFor(CREATE)

val Controller.bindView: Scope get() = scopeFor(BIND_VIEW)

val Controller.attach: Scope get() = scopeFor(ATTACH)

val Controller.detach: Scope get() = scopeFor(DETACH)

val Controller.unbindView: Scope get() = scopeFor(UNBIND_VIEW)

val Controller.destroy: Scope get() = scopeFor(DESTROY)