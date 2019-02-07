package com.ivianuu.director.sample.controller

import android.graphics.PorterDuff.Mode
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.activity
import com.ivianuu.director.handler
import com.ivianuu.director.push
import com.ivianuu.director.resources
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.CityGridSharedElementTransitionChangeHandler
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.buildModels
import com.ivianuu.director.sample.util.bundleOf
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_city_grid.img_dot
import kotlinx.android.synthetic.main.controller_city_grid.recycler_view
import kotlinx.android.synthetic.main.controller_city_grid.tv_title
import kotlinx.android.synthetic.main.row_city_grid.grid_image
import kotlinx.android.synthetic.main.row_city_grid.grid_title

class CityGridController : BaseController() {

    private val dotColor by lazy { args.getInt(KEY_DOT_COLOR) }
    private val fromPosition by lazy { args.getInt(KEY_FROM_POSITION) }

    override val layoutRes get() = R.layout.controller_city_grid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = args.getString(KEY_TITLE)
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)

        tv_title.text = actionBarTitle
        img_dot.drawable.setColorFilter(
            ContextCompat.getColor(activity, dotColor),
            Mode.SRC_ATOP
        )

        tv_title.transitionName =
                resources.getString(R.string.transition_tag_title_indexed, fromPosition)

        img_dot.transitionName =
                resources.getString(R.string.transition_tag_dot_indexed, fromPosition)

        recycler_view.layoutManager = GridLayoutManager(activity, 2)
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
            resources.getString(R.string.transition_tag_image_named, city.title),
            resources.getString(R.string.transition_tag_title_named, city.title)
        )

        router.push {
            controller(CityDetailController.newInstance(city.drawableRes, city.title))
            handler(CityGridSharedElementTransitionChangeHandler(names))
        }
    }

    companion object {
        private const val KEY_TITLE = "CityGridController.title"
        private const val KEY_DOT_COLOR = "CityGridController.dotColor"
        private const val KEY_FROM_POSITION = "CityGridController.position"

        private val CITIES = arrayOf(
            City("Chicago", R.drawable.chicago),
            City("Jakarta", R.drawable.jakarta),
            City("London", R.drawable.london),
            City("Sao Paulo", R.drawable.sao_paulo),
            City("Tokyo", R.drawable.tokyo)
        )

        fun newInstance(title: String, dotColor: Int, fromPosition: Int) = CityGridController().apply {
            args = bundleOf(
                KEY_TITLE to title,
                KEY_DOT_COLOR to dotColor,
                KEY_FROM_POSITION to fromPosition
            )
        }
    }
}

data class City(val title: String, val drawableRes: Int)

@EpoxyModelClass(layout = R.layout.row_city_grid)
abstract class CityModel : BaseEpoxyModel() {

    @EpoxyAttribute lateinit var city: City

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            grid_image.setImageResource(city.drawableRes)
            grid_title.text = city.title

            grid_image.transitionName = grid_image.resources
                .getString(R.string.transition_tag_image_named, city.title)

            grid_title.transitionName = grid_image.resources
                .getString(R.string.transition_tag_title_named, city.title)
        }
    }
}