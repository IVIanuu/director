package com.ivianuu.director.sample.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.bundleOf
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.controller_city_detail.*
import kotlinx.android.synthetic.main.row_city_detail.view.*
import kotlinx.android.synthetic.main.row_city_header.*

class CityDetailController : BaseController() {

    override val layoutRes get() = R.layout.controller_city_detail

    override fun onCreate() {
        super.onCreate()
        title = args.getString(KEY_TITLE)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = CityDetailAdapter(
                LayoutInflater.from(requireActivity()),
                title!!,
                args.getInt(KEY_IMAGE),
                LIST_ROWS,
                title!!
            )
        }
    }

    private class CityDetailAdapter(
        private val inflater: LayoutInflater,
        private val title: String,
        private val imageDrawableRes: Int,
        private val details: Array<String>,
        transitionNameBase: String
    ) : RecyclerView.Adapter<CityDetailAdapter.ViewHolder>() {
        private val imageViewTransitionName = inflater.context.resources.getString(
            R.string.transition_tag_image_named,
            transitionNameBase
        )
        private val textViewTransitionName = inflater.context.resources.getString(
            R.string.transition_tag_title_named,
            transitionNameBase
        )

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                VIEW_TYPE_HEADER
            } else {
                VIEW_TYPE_DETAIL
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return if (viewType == VIEW_TYPE_HEADER) {
                HeaderViewHolder(inflater.inflate(R.layout.row_city_header, parent, false))
            } else {
                DetailViewHolder(inflater.inflate(R.layout.row_city_detail, parent, false))
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (getItemViewType(position) == VIEW_TYPE_HEADER) {
                (holder as HeaderViewHolder).bind(
                    imageDrawableRes,
                    title,
                    imageViewTransitionName,
                    textViewTransitionName
                )
            } else {
                (holder as DetailViewHolder).bind(details[position - 1])
            }
        }

        override fun getItemCount() = 1 + details.size

        open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        class HeaderViewHolder(itemView: View) : ViewHolder(itemView), LayoutContainer {

            override val containerView: View?
                get() = itemView

            fun bind(
                imageDrawableRes: Int, title: String,
                imageTransitionName: String,
                textViewTransitionName: String
            ) {
                image_view.setImageResource(imageDrawableRes)
                text_view.text = title

                ViewCompat.setTransitionName(image_view, imageTransitionName)
                ViewCompat.setTransitionName(text_view, textViewTransitionName)
            }
        }

        class DetailViewHolder(itemView: View) : ViewHolder(itemView), LayoutContainer {

            override val containerView: View?
                get() = itemView

            fun bind(detail: String) {
                itemView.text_view.text = detail
            }
        }

        companion object {
            private const val VIEW_TYPE_HEADER = 0
            private const val VIEW_TYPE_DETAIL = 1
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