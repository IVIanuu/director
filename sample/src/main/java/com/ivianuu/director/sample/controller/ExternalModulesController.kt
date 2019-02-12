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
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.context
import com.ivianuu.director.push
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.buildModels
import com.ivianuu.director.toTransaction
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_external_modules.recycler_view
import kotlinx.android.synthetic.main.row_home.home_image
import kotlinx.android.synthetic.main.row_home.home_title

class ExternalModulesController : BaseController() {

    override val layoutRes: Int
        get() = R.layout.controller_external_modules

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "External Module Demo"
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)

        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.buildModels {
            AdditionalModuleItem.values().forEach { item ->
                additionalModuleItem {
                    id(item.toString())
                    item(item)
                    onClick { onItemClicked(item) }
                }
            }
        }
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

    private fun onItemClicked(item: AdditionalModuleItem) {
        when (item) {
            AdditionalModuleItem.ARCH -> {
                router.push(ArchController().toTransaction())
            }
            AdditionalModuleItem.SCOPES -> {
                router.push(ScopesController().toTransaction())
            }
            AdditionalModuleItem.TRAVELER -> {
                router.push(TravelerController().toTransaction())
            }
        }
    }

}

enum class AdditionalModuleItem(val title: String, val color: Int) {
    ARCH("Arch", R.color.red_300),
    SCOPES("Scopes", R.color.blue_grey_300),
    TRAVELER("Traveler", R.color.purple_300)
}

@EpoxyModelClass(layout = R.layout.row_home)
abstract class AdditionalModuleItemModel : BaseEpoxyModel() {

    @EpoxyAttribute lateinit var item: AdditionalModuleItem

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            home_title.text = item.title
            home_image.drawable.setColorFilter(
                ContextCompat.getColor(containerView.context, item.color),
                PorterDuff.Mode.SRC_ATOP
            )
        }
    }
}