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

import android.view.ViewGroup

/**
 * @author Manuel Wrage (IVIanuu)
 */
abstract class ViewGroupRouter(
    id: Int,
    tag: String?,
    routerManager: RouterManager
) : BaseRouter(id, tag, routerManager) {

    /**
     * The container of this router
     */
    var container: ViewGroup? = null
        private set

    protected open fun onContainerSet(container: ViewGroup) {
    }

    protected open fun onContainerRemoved(container: ViewGroup) {
    }

    override fun onDestroy() {
        super.onDestroy()
        removeContainer()
    }

    /**
     * Sets the container of this router
     */
    fun setContainer(container: ViewGroup) {
        require(container.id == id) {
            "container id of the container must match the container id of this router"
        }

        if (this.container != container) {
            removeContainer()
            this.container = container
            onContainerSet(container)
        }
    }

    /**
     * Removes the current container if set
     */
    fun removeContainer() {
        val container = container ?: return
        onContainerRemoved(container)
        this.container = null
    }

}

val ViewGroupRouter.hasContainer: Boolean get() = container != null