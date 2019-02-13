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

/**
 * Global defaults
 */
object DirectorPlugins

private var _defaultControllerFactory: ControllerFactory? = null

/** Default controller factory to use */
var DirectorPlugins.defaultControllerFactory: ControllerFactory?
    get() = _defaultControllerFactory
    set(value) {
        _defaultControllerFactory = value
    }

private var _defaultPopsLastView = false

/** Default pops last view */
var DirectorPlugins.defaultPopsLastView: Boolean
    get() = _defaultPopsLastView
    set(value) {
        _defaultPopsLastView = value
    }

private var _defaultBlockTouchesOnTransactions = true

/** Default block touches on transactions */
var DirectorPlugins.defaultBlockTouchesOnTransactions: Boolean
    get() = _defaultBlockTouchesOnTransactions
    set(value) {
        _defaultBlockTouchesOnTransactions = value
    }

private var _defaultBlockBackClicksOnTransactions = true

/** Default block back clicks on transactions */
var DirectorPlugins.defaultBlockBlackClicksOnTransactions: Boolean
    get() = _defaultBlockBackClicksOnTransactions
    set(value) {
        _defaultBlockBackClicksOnTransactions = value
    }

private var _defaultRetainView = false

/** Default retain view */
var DirectorPlugins.defaultRetainView: Boolean
    get() = _defaultRetainView
    set(value) {
        _defaultRetainView = value
    }

private var _defaultPushHandler: ControllerChangeHandler? = null

/** The default push handler to use in all transactions */
var DirectorPlugins.defaultPushHandler: ControllerChangeHandler?
    get() = _defaultPushHandler
    set(value) {
        _defaultPushHandler = value
    }

private var _defaultPopHandler: ControllerChangeHandler? = null

/** The default push handler to use in all transactions */
var DirectorPlugins.defaultPopHandler: ControllerChangeHandler?
    get() = _defaultPopHandler
    set(value) {
        _defaultPopHandler = value
    }

/** Sets the [handler] as the default push and pop handler */
fun DirectorPlugins.setDefaultHandler(handler: ControllerChangeHandler?) {
    defaultPushHandler = handler
    defaultPopHandler = handler
}

private var _defaultRemovesFromViewOnPush = true

/** Default removes from view on push */
var DirectorPlugins.defaultRemovesFromViewOnPush: Boolean
    get() = _defaultRemovesFromViewOnPush
    set(value) {
        _defaultRemovesFromViewOnPush = value
    }