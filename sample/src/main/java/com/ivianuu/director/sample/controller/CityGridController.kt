package com.ivianuu.director.sample.controller

import android.graphics.PorterDuff.Mode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.common.changehandler.TransitionChangeHandlerCompat
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.requireActivity
import com.ivianuu.director.requireResources
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.CityGridSharedElementTransitionChangeHandler
import com.ivianuu.director.sample.util.bundleOf
import com.ivianuu.director.toTransaction
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.controller_city_grid.*
import kotlinx.android.synthetic.main.row_city_grid.view.*

class CityGridController : BaseController() {

    private val dotColor by lazy { args.getInt(KEY_DOT_COLOR) }
    private val fromPosition by lazy { args.getInt(KEY_FROM_POSITION) }

    override val layoutRes get() = R.layout.controller_city_grid

    override fun onCreate() {
        super.onCreate()
        title = args.getString(KEY_TITLE)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        tv_title.text = title
        img_dot.drawable.setColorFilter(
            ContextCompat.getColor(requireActivity(), dotColor),
            Mode.SRC_ATOP
        )

        ViewCompat.setTransitionName(
            tv_title,
            requireResources().getString(R.string.transition_tag_title_indexed, fromPosition)
        )
        ViewCompat.setTransitionName(
            img_dot,
            requireResources().getString(R.string.transition_tag_dot_indexed, fromPosition)
        )

        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 2)
            adapter = CityGridAdapter(LayoutInflater.from(context), CITY_MODELS)
        }
    }

    private fun onModelRowClick(model: CityModel) {
        val names = listOf(
            requireResources().getString(R.string.transition_tag_image_named, model.title),
            requireResources().getString(R.string.transition_tag_title_named, model.title)
        )

        router.pushController(
            CityDetailController.newInstance(model.drawableRes, model.title).toTransaction()
                .pushChangeHandler(
                    TransitionChangeHandlerCompat(
                        CityGridSharedElementTransitionChangeHandler(names),
                        FadeChangeHandler()
                    )
                )
                .popChangeHandler(
                    TransitionChangeHandlerCompat(
                        CityGridSharedElementTransitionChangeHandler(names),
                        FadeChangeHandler()
                    )
                )
        )
    }

    private inner class CityGridAdapter(
        private val inflater: LayoutInflater,
        private val items: Array<CityModel>
    ) : RecyclerView.Adapter<CityGridAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(inflater.inflate(R.layout.row_city_grid, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int {
            return items.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

            override val containerView: View?
                get() = itemView

            fun bind(item: CityModel) {
                itemView.img_city.setImageResource(item.drawableRes)
                itemView.tv_title.text = item.title

                ViewCompat.setTransitionName(
                    itemView.tv_title,
                    requireResources().getString(R.string.transition_tag_title_named, item.title)
                )
                ViewCompat.setTransitionName(
                    itemView.img_city,
                    requireResources().getString(R.string.transition_tag_image_named, item.title)
                )

                itemView.setOnClickListener { onModelRowClick(item) }
            }
        }
    }

    data class CityModel(val drawableRes: Int, val title: String)

    companion object {
        private const val KEY_TITLE = "CityGridController.title"
        private const val KEY_DOT_COLOR = "CityGridController.dotColor"
        private const val KEY_FROM_POSITION = "CityGridController.position"

        private val CITY_MODELS = arrayOf(
            CityModel(R.drawable.chicago, "Chicago"),
            CityModel(R.drawable.jakarta, "Jakarta"),
            CityModel(R.drawable.london, "London"),
            CityModel(R.drawable.sao_paulo, "Sao Paulo"),
            CityModel(R.drawable.tokyo, "Tokyo")
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
