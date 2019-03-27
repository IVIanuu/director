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

package com.ivianuu.director.pager

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.ivianuu.director.Router
import com.ivianuu.director.RouterManager
import com.ivianuu.director.getRouter
import com.ivianuu.director.hasRoot

/**
 * An adapter for ViewPagers that uses Routers as pages
 */
abstract class RouterPagerAdapter(
    private val manager: RouterManager
) : PagerAdapter() {

    private val visibleRouters = SparseArray<Router>()
    private val savedStates = SparseArray<Bundle>()

    /**
     * Called when a router is instantiated. Here the router's root should be set if needed.
     */
    abstract fun configureRouter(router: Router, position: Int)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val name = (container.id + position).toString()

        val router = manager.getRouter(container, name)
        if (!router.hasRoot) {
            val routerSavedState = savedStates.get(position)

            if (routerSavedState != null) {
                router.restoreInstanceState(routerSavedState)
                savedStates.remove(position)
            }
        }

        router.rebind()
        configureRouter(router, position)

        visibleRouters.put(position, router)
        return router
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val router = `object` as Router

        savedStates.put(position, router.saveInstanceState())

        manager.removeRouter(router)

        visibleRouters.remove(position)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean =
        (`object` as Router).backstack.any { it.controller.view == view }

    override fun saveState(): Parcelable {
        val bundle = Bundle()
        bundle.putSparseParcelableArray(KEY_SAVED_PAGES, savedStates)
        return bundle
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        super.restoreState(state, loader)
        val bundle = state as? Bundle
        if (bundle != null) {
            savedStates.clear()
            bundle.getSparseParcelableArray<Bundle>(KEY_SAVED_PAGES)?.let { pages ->
                (0 until pages.size())
                    .map(pages::valueAt)
                    .forEachIndexed(savedStates::setValueAt)
            }
        }
    }

    /**
     * Returns the already instantiated Router in the specified position or `null` if there
     * is no router associated with this position.
     */
    fun getRouter(position: Int): Router? = visibleRouters.get(position)

    companion object {
        private const val KEY_SAVED_PAGES = "RouterPagerAdapter.savedStates"
    }
}