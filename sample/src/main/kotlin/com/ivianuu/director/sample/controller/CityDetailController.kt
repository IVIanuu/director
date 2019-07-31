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

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.buildModels
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_city_detail.*
import kotlinx.android.synthetic.main.row_city_detail.*
import kotlinx.android.synthetic.main.row_city_header.*

class CityDetailController(
    private val title: String,
    private val imageRes: Int
) : BaseController() {

    override val layoutRes get() = R.layout.controller_city_detail
    override val toolbarTitle: String?
        get() = title

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        val imageViewTransitionName = view.resources.getString(
            R.string.transition_tag_image_named,
            title
        )

        val textViewTransitionName = view.resources.getString(
            R.string.transition_tag_title_named,
            title
        )

        recycler_view.layoutManager = LinearLayoutManager(requireActivity())

        recycler_view.buildModels {
            cityHeader {
                id("header")
                imageRes(imageRes)
                title(title)
                imageTransitionName(imageViewTransitionName)
                textViewTransitionName(textViewTransitionName)
            }

            LIST_ROWS.forEach { listRow ->
                cityDetail {
                    id("city_detail_$listRow")
                    text(listRow)
                }
            }
        }
    }

    companion object {
        private val LIST_ROWS = arrayOf(
            "• This is a city.",
            "• There's some cool stuff about it.",
            "• But really this is just a demo, not a city guide app.",
            "• This demo is meant to show some nice transitions, as long as you're on Lollipop or later.",
            "• You should have seen some sweet shared element transitions using the ImageView and the TextView in the \"header\" above.",
            "• This transition utilized some callbacks to ensure all the necessary rows in the RecyclerView were laid about before the transition occurred.",
            "• Just adding some more lines so it scrolls now...\n\n\n\n\n\n\nThe end."
        )
    }
}

@EpoxyModelClass(layout = R.layout.row_city_header)
abstract class CityHeaderModel : BaseEpoxyModel() {

    @EpoxyAttribute
    var imageRes: Int = 0
    @EpoxyAttribute
    lateinit var title: String
    @EpoxyAttribute
    lateinit var imageTransitionName: String
    @EpoxyAttribute
    lateinit var textViewTransitionName: String

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            header_image.setImageResource(imageRes)
            header_image.transitionName = imageTransitionName
            header_title.text = title
            header_title.transitionName = textViewTransitionName
        }
    }
}

@EpoxyModelClass(layout = R.layout.row_city_detail)
abstract class CityDetailModel : BaseEpoxyModel() {

    @EpoxyAttribute
    lateinit var text: String

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        holder.detail_text.text = text
    }
}