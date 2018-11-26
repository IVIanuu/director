package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.resources
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.arg
import com.ivianuu.director.sample.util.buildModels
import com.ivianuu.director.sample.util.bundleOf
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_city_detail.recycler_view
import kotlinx.android.synthetic.main.row_city_detail.detail_text
import kotlinx.android.synthetic.main.row_city_header.header_image
import kotlinx.android.synthetic.main.row_city_header.header_title

class CityDetailController : BaseController() {

    override val layoutRes get() = R.layout.controller_city_detail

    private val title by arg<String>(KEY_TITLE)
    private val image by arg<Int>(KEY_IMAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = title
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        val imageViewTransitionName = resources.getString(
            R.string.transition_tag_image_named,
            title
        )

        val textViewTransitionName = resources.getString(
            R.string.transition_tag_title_named,
            title
        )

        recycler_view.layoutManager = LinearLayoutManager(activity)

        recycler_view.buildModels {
            cityHeader {
                id("header")
                imageDrawableRes(image)
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
        private const val KEY_TITLE = "CityDetailController.title"
        private const val KEY_IMAGE = "CityDetailController.image"

        private val LIST_ROWS = arrayOf(
            "• This is a city.",
            "• There's some cool stuff about it.",
            "• But really this is just a demo, not a city guide app.",
            "• This demo is meant to show some nice transitions, as long as you're on Lollipop or later.",
            "• You should have seen some sweet shared element transitions using the ImageView and the TextView in the \"header\" above.",
            "• This transition utilized some callbacks to ensure all the necessary rows in the RecyclerView were laid about before the transition occurred.",
            "• Just adding some more lines so it scrolls now...\n\n\n\n\n\n\nThe end."
        )

        fun newInstance(imageDrawableRes: Int, title: String) = CityDetailController().apply {
            args = bundleOf(
                KEY_IMAGE to imageDrawableRes,
                KEY_TITLE to title
            )
        }
    }
}

@EpoxyModelClass(layout = R.layout.row_city_header)
abstract class CityHeaderModel : BaseEpoxyModel() {

    @EpoxyAttribute var imageDrawableRes: Int = 0
    @EpoxyAttribute lateinit var title: String
    @EpoxyAttribute lateinit var imageTransitionName: String
    @EpoxyAttribute lateinit var textViewTransitionName: String

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            header_image.setImageResource(imageDrawableRes)
            header_image.transitionName = imageTransitionName
            header_title.text = title
            header_title.transitionName = textViewTransitionName
        }
    }
}

@EpoxyModelClass(layout = R.layout.row_city_detail)
abstract class CityDetailModel : BaseEpoxyModel() {

    @EpoxyAttribute lateinit var text: String

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        holder.detail_text.text = text
    }
}