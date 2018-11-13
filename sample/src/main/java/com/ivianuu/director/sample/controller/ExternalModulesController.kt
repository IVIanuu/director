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

package com.ivianuu.director.sample.controller

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_external_modules.*
import kotlinx.android.synthetic.main.row_home.view.*

class ExternalModulesController : BaseController() {

    override val layoutRes: Int
        get() = R.layout.controller_external_modules

    override fun onCreate() {
        super.onCreate()
        title = "External Module Demo"
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        recycler_view.setHasFixedSize(true)
        recycler_view.layoutManager = LinearLayoutManager(requireActivity())
        recycler_view.adapter = AdditionalModulesAdapter(DemoModel.values())
    }

    override fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeStarted(changeHandler, changeType)
        if (changeType.isEnter) {
            setTitle()
        }
    }

    fun onModelRowClick(model: DemoModel) {
        when (model) {
            DemoModel.SCOPES -> {
                router.pushController(
                    ScopesController().toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            DemoModel.TRAVELER -> {
                router.pushController(
                    TravelerController().toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
        }
    }

    enum class DemoModel(val title: String, val color: Int) {
        SCOPES("Scopes", R.color.red_300),
        TRAVELER("Traveler", R.color.blue_grey_300),
        //AUTODISPOSE("Autodispose", R.color.purple_300),
        //ARCH_LIFECYCLE("Arch Components Lifecycle", R.color.orange_300)
    }

    inner class AdditionalModulesAdapter(
        private val items: Array<DemoModel>
    ) : RecyclerView.Adapter<AdditionalModulesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_home, parent, false)
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bind(item: DemoModel) {
                itemView.tv_title.text = item.title
                itemView.img_dot.drawable.setColorFilter(
                    ContextCompat.getColor(itemView.context, item.color),
                    PorterDuff.Mode.SRC_ATOP
                )
                itemView.setOnClickListener { onModelRowClick(item) }
            }
        }
    }
}