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

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.ivianuu.director.Router

/**
 * A [PagerAdapter] that uses [router]s as pages
 */
abstract class RouterPagerAdapter(
    private val routerFactory: () -> Router
) : PagerAdapter() {

    private val routers = mutableMapOf<Int, Router>()

    /**
     * Configure the router e.g. set the root controller
     */
    abstract fun configureRouter(router: Router, position: Int)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val router = routers.getOrPut(position, routerFactory)
        router.setContainer(container)
        configureRouter(router, position)
        return router
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        (`object` as Router).removeContainer()
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean =
        (`object` as Router).backstack.any { it.controller.view == view }

    /**
     * Returns the already instantiated router in the specified position or `null` if there
     * is no router associated with this position.
     */
    fun getRouter(position: Int): Router = routers.getValue(position)

}