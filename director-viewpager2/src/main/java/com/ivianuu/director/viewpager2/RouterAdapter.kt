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

package com.ivianuu.director.viewpager2

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.StatefulAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ivianuu.director.Controller
import com.ivianuu.director.Router
import com.ivianuu.director.RouterManager
import com.ivianuu.director.hasContainer
import com.ivianuu.director.hasRoot

/**
 * A [RecyclerView.Adapter] for [Controller]s which can be used with the [ViewPager2]
 */
abstract class RouterAdapter(
    private val manager: RouterManager
) : RecyclerView.Adapter<RouterAdapter.RouterViewHolder>(), StatefulAdapter {

    private val visibleRouters = SparseArray<Router>()
    private val savedStates = SparseArray<Bundle>()

    private val dataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            restoreState(saveState())
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        registerAdapterDataObserver(dataObserver)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        unregisterAdapterDataObserver(dataObserver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouterViewHolder =
        createHolder(parent)

    override fun onBindViewHolder(holder: RouterViewHolder, position: Int) {
        val router = getRouter(position, holder.container.id)
        holder.router = router

        val container = holder.container

        if (ViewCompat.isAttachedToWindow(container)) {
            if (container.parent != null) {
                throw IllegalStateException("Design assumption violated.")
            }
            container.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View, left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                ) {
                    if (container.parent != null) {
                        container.removeOnLayoutChangeListener(this)
                        onViewAttachedToWindow(holder)
                    }
                }
            })
        }
    }

    abstract fun configureRouter(router: Router, position: Int)

    override fun onViewAttachedToWindow(holder: RouterViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.router?.let {
            if (!it.hasContainer) {
                it.setContainer(holder.container)
                it.rebind()
            }
        }
    }

    override fun onViewRecycled(holder: RouterViewHolder) {
        super.onViewRecycled(holder)
        removeRouter(holder)
    }

    override fun onFailedToRecycleView(holder: RouterViewHolder): Boolean {
        removeRouter(holder)
        return false // don't recycle the view
    }

    private fun getRouter(position: Int, containerId: Int): Router {
        val router = manager.getRouter(containerId)

        if (!router.hasRoot) {
            val routerSavedState = savedStates[position]

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

    private fun removeRouter(holder: RouterViewHolder) {
        val router = holder.router ?: return
        val itemId = holder.itemId
        if (router.hasRoot && containsItem(itemId)) {
            savedStates.put(itemCount, router.saveInstanceState())
        }

        visibleRouters.remove(itemCount)
        manager.removeRouter(router)
    }

    override fun getItemId(position: Int): Long = position.toLong()

    open fun containsItem(itemId: Long): Boolean = itemId in 0..(itemCount - 1)

    final override fun setHasStableIds(hasStableIds: Boolean) {
        throw UnsupportedOperationException(
            "stable ids are required for the adapter to function properly, " +
                    "and the adapter takes care of setting the flag."
        )
    }

    override fun restoreState(savedState: Parcelable) {
        if (savedState !is Bundle) return

        savedStates.clear()

        savedState.getSparseParcelableArray<Bundle>(KEY_ROUTER_STATES)?.let { states ->
            (0 until states.size())
                .map { states.valueAt(it) }
                .forEachIndexed { index, bundle -> savedStates.setValueAt(index, bundle) }
        }
    }

    override fun saveState(): Parcelable = Bundle().apply {
        putSparseParcelableArray(KEY_ROUTER_STATES, savedStates)
    }

    /**
     * Returns the already instantiated Router in the specified position or `null` if there
     * is no router associated with this position.
     */
    fun getRouter(position: Int): Router? = visibleRouters.get(position)

    private fun createHolder(
        parent: ViewGroup
    ): RouterViewHolder {
        val container = FrameLayout(parent.context)
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.id = ViewCompat.generateViewId()
        container.isSaveEnabled = false
        return RouterViewHolder(container)
    }


    class RouterViewHolder internal constructor(
        val container: FrameLayout
    ) : RecyclerView.ViewHolder(container) {
        var router: Router? = null
    }

    private companion object {
        private const val KEY_ROUTER_STATES = "RouterPagerAdapter.routerStates"
    }
}