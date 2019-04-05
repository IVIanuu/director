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

import android.os.Bundle
import com.ivianuu.closeable.Closeable
import com.ivianuu.director.internal.newInstanceOrThrow
import com.ivianuu.stdlibx.firstNotNullResultOrNull
import com.ivianuu.stdlibx.safeAs

/**
 * @author Manuel Wrage (IVIanuu)
 */
abstract class BaseRouter {

    var id: Int = 0
        internal set
    var tag: String? = null
        internal set

    val routerManager: RouterManager
        get() {
            check(this::_routerManager.isInitialized) {
                "Can't be accessed before onCreate()"
            }

            return _routerManager
        }
    private lateinit var _routerManager: RouterManager

    abstract val controllers: List<Controller>

    private val listeners =
        mutableListOf<ListenerEntry<RouterListener>>()
    private val controllerListeners =
        mutableListOf<ListenerEntry<ControllerListener>>()

    protected open fun onCreate(savedInstanceState: Bundle?) {
    }

    protected open fun onDestroy() {
    }

    protected open fun onStart() {
    }

    protected open fun onStop() {
    }

    protected open fun onRestoreInstanceState(savedInstanceState: Bundle) {
    }

    protected open fun onSaveInstanceState(outState: Bundle) {
    }

    open fun handleBack(): Boolean = false

    /**
     * Notifies the [listener] on controller changes
     */
    fun addListener(listener: RouterListener, recursive: Boolean = false): Closeable {
        listeners.add(ListenerEntry(listener, recursive))
        return Closeable { removeListener(listener) }
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeListener(listener: RouterListener) {
        listeners.removeAll { it.listener == listener }
    }

    /**
     * Adds the [listener] to all controllers
     */
    fun addControllerListener(listener: ControllerListener, recursive: Boolean = false): Closeable {
        controllerListeners.add(ListenerEntry(listener, recursive))
        return Closeable { removeControllerListener(listener) }
    }

    /**
     * Removes the previously added [listener]
     */
    fun removeControllerListener(listener: ControllerListener) {
        controllerListeners.removeAll { it.listener == listener }
    }

    internal fun getListeners(recursiveOnly: Boolean = false): List<RouterListener> {
        return listeners
            .filter { !recursiveOnly || it.recursive }
            .map(ListenerEntry<RouterListener>::listener) +
                (routerManager.host.safeAs<Controller>()?.router?.getListeners(true)
                    ?: emptyList())
    }

    internal fun getControllerListeners(recursiveOnly: Boolean = false): List<ControllerListener> {
        return controllerListeners
            .filter { !recursiveOnly || it.recursive }
            .map(ListenerEntry<ControllerListener>::listener) +
                (routerManager.host.safeAs<Controller>()?.router?.getControllerListeners(true)
                    ?: emptyList())
    }

    internal fun create(routerManager: RouterManager, savedInstanceState: Bundle?) {
        _routerManager = routerManager

        val savedState = savedInstanceState?.getBundle(KEY_SAVED_STATE)
        onCreate(savedState)

        savedInstanceState?.let(this::restoreInstanceState)
    }

    internal fun destroy() {
        onDestroy()
    }

    internal fun start() {
        onStart()
    }

    internal fun stop() {
        onStop()
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {

    }

    fun saveInstanceState(): Bundle {
    }

    private data class ListenerEntry<T>(
        val listener: T,
        val recursive: Boolean
    )

    companion object {
        private const val KEY_CLASS_NAME = "BaseRouter.className"
        private const val KEY_ID = "BaseRouter.id"
        private const val KEY_TAG = "BaseRouter.tag"
        private const val KEY_SAVED_STATE = "BaseRouter.savedState"

        fun fromBundle(bundle: Bundle): BaseRouter {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            return newInstanceOrThrow<BaseRouter>(className).apply {
                id = bundle.getInt(KEY_ID)
                tag = bundle.getString(KEY_TAG)
                savedState = bundle.getBundle(KEY_SAVED_STATE)
            }
        }
    }

}

fun BaseRouter.getControllerByTagOrNull(tag: String): Controller? =
    controllers.firstNotNullResultOrNull {
        if (it.tag == tag) {
            it
        } else {
            it.childRouterManager
                .getControllerByTagOrNull(tag)
        }
    }

fun BaseRouter.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun BaseRouter.getControllerByInstanceIdOrNull(instanceId: String): Controller? =
    controllers.firstNotNullResultOrNull {
        if (it.instanceId == instanceId) {
            it
        } else {
            it.childRouterManager
                .getControllerByInstanceIdOrNull(instanceId)
        }
    }

fun BaseRouter.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")