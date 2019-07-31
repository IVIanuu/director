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

import android.graphics.PorterDuff.Mode
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.changeHandler
import com.ivianuu.director.push
import com.ivianuu.director.requireActivity
import com.ivianuu.director.requireView
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.CityGridSharedElementTransitionChangeHandler
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.buildModels

import com.ivianuu.director.toTransaction
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_city_grid.*
import kotlinx.android.synthetic.main.row_city_grid.*

class CityGridController(
    private val title: String,
    private val dotColor: Int,
    private val fromPosition: Int
) : BaseController() {

    override val layoutRes get() = R.layout.controller_city_grid
    override val toolbarTitle: String?
        get() = title

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        tv_title.text = toolbarTitle
        img_dot.drawable.setColorFilter(
            ContextCompat.getColor(requireActivity(), dotColor),
            Mode.SRC_ATOP
        )

        tv_title.transitionName =
            view.resources.getString(R.string.transition_tag_title_indexed, fromPosition)

        img_dot.transitionName =
            view.resources.getString(R.string.transition_tag_dot_indexed, fromPosition)

        recycler_view.layoutManager = GridLayoutManager(requireActivity(), 2)
        recycler_view.buildModels {
            CITIES.forEach { city ->
                city {
                    id(city.title)
                    city(city)
                    onClick { onCityClicked(city) }
                }
            }
        }
    }

    private fun onCityClicked(city: City) {
        val names = listOf(
            requireView().resources.getString(R.string.transition_tag_image_named, city.title),
            requireView().resources.getString(R.string.transition_tag_title_named, city.title)
        )

        router.push(
            CityDetailController(city.title, city.imageRes)
                .toTransaction()
                .changeHandler(CityGridSharedElementTransitionChangeHandler(names))
        )
    }

    companion object {
        private val CITIES = arrayOf(
            City("Chicago", R.drawable.chicago),
            City("Jakarta", R.drawable.jakarta),
            City("London", R.drawable.london),
            City("Sao Paulo", R.drawable.sao_paulo),
            City("Tokyo", R.drawable.tokyo)
        )
    }
}

data class City(val title: String, val imageRes: Int)

@EpoxyModelClass(layout = R.layout.row_city_grid)
abstract class CityModel : BaseEpoxyModel() {

    @EpoxyAttribute
    lateinit var city: City

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            grid_image.setImageResource(city.imageRes)
            grid_title.text = city.title

            grid_image.transitionName = grid_image.resources
                .getString(R.string.transition_tag_image_named, city.title)

            grid_title.transitionName = grid_image.resources
                .getString(R.string.transition_tag_title_named, city.title)
        }
    }
}