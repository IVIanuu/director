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
import android.view.ViewGroup
import com.ivianuu.closeable.Closeable
import com.ivianuu.director.internal.ControllerChangeManager
import com.ivianuu.director.internal.newInstanceOrThrow
import com.ivianuu.stdlibx.firstNotNullResultOrNull
import com.ivianuu.stdlibx.safeAs

/**
 * @author Manuel Wrage (IVIanuu)
 */
abstract class Router {

    abstract val transactions: List<Transaction>

    var container: ViewGroup? = null
        private set

    val routerManager: RouterManager
        get() {
            check(this::_routerManager.isInitialized) {
                "routerManager cannot be accessed be onCreate()"
            }
            return _routerManager
        }
    private lateinit var _routerManager: RouterManager

    private var allState: Bundle? = null

    var containerId: Int = 0
        internal set
    var tag: String? = null
        internal set

    private var isStarted = false
    private var isDestroyed = false

    private val listeners =
        mutableListOf<ListenerEntry<RouterListener>>()
    private val controllerListeners =
        mutableListOf<ListenerEntry<ControllerListener>>()

    protected open fun onCreate(savedInstanceState: Bundle?) {
    }

    protected open fun onDestroy() {
    }

    protected open fun onContainerSet(container: ViewGroup) {
    }

    protected open fun onContainerRemoved(container: ViewGroup) {
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

    internal fun create(routerManager: RouterManager) {
        if (this::_routerManager.isInitialized) return

        _routerManager = routerManager

        allState?.let {
            containerId = it.getInt(KEY_CONTAINER_ID)
            tag = it.getString(KEY_TAG)
        }

        check(containerId != 0) { "containerId must be specified" }

        val savedState = allState?.getBundle(KEY_SAVED_STATE)

        onCreate(savedState)
        savedState?.let(this::onRestoreInstanceState)

        allState = null
    }

    internal fun destroy() {
        if (!isDestroyed) {
            isDestroyed = true
            onDestroy()
        }
    }

    fun setContainer(container: ViewGroup) {
        require(container.id == containerId) {
            "container id of the container must match the container id of this router"
        }

        if (this.container != container) {
            removeContainer()
            this.container = container
            onContainerSet(container)
        }
    }

    fun removeContainer() {
        val container = container ?: return
        prepareForContainerRemoval()
        onContainerRemoved(container)
        this.container = null
    }

    internal fun start() {
        if (!isStarted) {
            isStarted = true
            onStart()
        }
    }

    internal fun stop() {
        if (isStarted) {
            isStarted = false
            prepareForContainerRemoval()
            onStop()
        }
    }

    fun saveInstanceState(): Bundle {
        prepareForContainerRemoval()

        val bundle = Bundle()
        bundle.putString(KEY_CLASS_NAME, javaClass.name)
        bundle.putInt(KEY_CONTAINER_ID, containerId)
        bundle.putString(KEY_TAG, tag)

        val savedState = Bundle()
        onSaveInstanceState(savedState)
        bundle.putBundle(KEY_SAVED_STATE, savedState)

        return bundle
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {
        onRestoreInstanceState(savedInstanceState.getBundle(KEY_SAVED_STATE)!!)
    }

    protected fun prepareForContainerRemoval() {
        transactions.reversed().forEach {
            ControllerChangeManager.cancelChange(it.controller.instanceId)
        }
    }

    protected fun performControllerChange(
        from: Transaction?,
        to: Transaction?,
        isPush: Boolean,
        handler: ChangeHandler? = null,
        forceRemoveFromViewOnPush: Boolean,
        toIndex: Int
    ) {
        val container = container ?: return
        ControllerChangeManager.executeChange(
            this,
            from?.controller,
            to?.controller,
            isPush,
            container,
            handler,
            forceRemoveFromViewOnPush,
            toIndex,
            getListeners()
        )
    }

    protected fun moveControllerToCorrectState(controller: Controller) {
        controller.create(this)

        if (isStarted) {
            controller.attach()
        }

        if (isDestroyed) {
            controller.destroy()
        }
    }

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

    private data class ListenerEntry<T>(
        val listener: T,
        val recursive: Boolean
    )

    companion object {
        private const val KEY_CLASS_NAME = "Router.className"
        private const val KEY_CONTAINER_ID = "Router.containerId"
        private const val KEY_TAG = "Router.tag"
        private const val KEY_SAVED_STATE = "Router.allState"

        fun fromBundle(bundle: Bundle): Router {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            return newInstanceOrThrow<Router>(className).apply {
                allState = bundle
            }
        }
    }

}

val Router.hasContainer: Boolean get() = container != null

fun Router.getControllerByTagOrNull(tag: String): Controller? =
    transactions.firstNotNullResultOrNull {
        if (it.tag == tag) {
            it.controller
        } else {
            it.controller.childRouterManager
                .getControllerByTagOrNull(tag)
        }
    }

fun Router.getControllerByTag(tag: String): Controller =
    getControllerByTagOrNull(tag) ?: error("couldn't find controller for tag: $tag")

fun Router.getControllerByInstanceIdOrNull(instanceId: String): Controller? =
    transactions.firstNotNullResultOrNull {
        if (it.controller.instanceId == instanceId) {
            it.controller
        } else {
            it.controller.childRouterManager
                .getControllerByInstanceIdOrNull(instanceId)
        }
    }

fun Router.getControllerByInstanceId(instanceId: String): Controller =
    getControllerByInstanceIdOrNull(instanceId)
        ?: error("couldn't find controller with instanceId: $instanceId")
