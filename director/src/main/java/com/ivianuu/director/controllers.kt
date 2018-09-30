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

fun Controller.toTransaction() = RouterTransaction(this)

val Controller.application get() = activity?.application

val Controller.resources get() = activity?.resources

fun Controller.requireApplication() =
    application?.applicationContext ?: throw IllegalStateException("not attached to a router")

fun Controller.requireActivity() =
    activity ?: throw IllegalStateException("not attached to a router")

fun Controller.requireParentController() =
    parentController ?: throw IllegalStateException("no parent controller set")

fun Controller.requireResources() =
    resources ?: throw IllegalStateException("not attached to a router")

fun Controller.requireTargetController() =
    targetController ?: throw IllegalStateException("no target controller set")

fun Controller.requireView() = view ?: throw IllegalStateException("no view is attached")