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

package com.ivianuu.director.common

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.ivianuu.director.Router
import com.ivianuu.director.RouterManager
import com.ivianuu.director.clear
import com.ivianuu.director.getRouter

/**
 * A [PagerAdapter] that uses [Router]s as pages
 */
abstract class RouterPagerAdapter(
    private val manager: RouterManager
) : PagerAdapter() {

    private val routers = SparseArray<Router>()

    /**
     * Configure the router e.g. set the root controller
     */
    abstract fun configureRouter(router: Router, position: Int)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val tag = (container.id + position).toString()

        val router = manager.getRouter(container, tag)
        configureRouter(router, position)

        routers.put(position, router)
        return router
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val router = `object` as Router

        router.clear()
        manager.removeRouter(router)

        routers.remove(position)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean =
        (`object` as Router).backstack.any { it.controller.view == view }

    /**
     * Returns the already instantiated Router in the specified position or `null` if there
     * is no router associated with this position.
     */
    fun getRouter(position: Int): Router = routers.get(position)

}