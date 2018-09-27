package com.ivianuu.director.sample.controllers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivianuu.director.common.HorizontalChangeHandler
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.controller_master_detail_list.*
import kotlinx.android.synthetic.main.row_detail_item.*

class MasterDetailListController : BaseController() {

    private var selectedIndex = 0
    private var twoPaneView = false

    override var title: String?
        get() = "Master/Detail Flow"
        set(value: String?) {
            super.title = value
        }

    override val layoutRes = R.layout.controller_master_detail_list

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = DetailItemAdapter(LayoutInflater.from(context), DetailItemModel.values())
        }

        twoPaneView = detail_container != null

        if (twoPaneView) {
            onRowSelected(selectedIndex)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_SELECTED_INDEX, selectedIndex)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        selectedIndex = savedInstanceState.getInt(KEY_SELECTED_INDEX)
    }

    fun onRowSelected(index: Int) {
        selectedIndex = index

        val model = DetailItemModel.values()[index]
        val controller = ChildController.newInstance(model.detail, model.backgroundColor, true)

        if (twoPaneView) {
            getChildRouter(detail_container).setRoot(controller.toTransaction())
        } else {
            router.pushController(
                controller.toTransaction()
                    .pushChangeHandler(HorizontalChangeHandler())
                    .popChangeHandler(HorizontalChangeHandler())
            )
        }
    }

    private inner class DetailItemAdapter(
        private val inflater: LayoutInflater,
        private val items: Array<DetailItemModel>
    ) : RecyclerView.Adapter<DetailItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_detail_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], position)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

            override val containerView: View?
                get() = itemView

            fun bind(item: DetailItemModel, position: Int) {
                tv_title.text = item.title
                if (twoPaneView && position == selectedIndex) {
                    row_root.setBackgroundColor(
                        ContextCompat.getColor(
                            row_root.context,
                            R.color.grey_400
                        )
                    )
                } else {
                    row_root.setBackgroundColor(
                        ContextCompat.getColor(
                            row_root.context,
                            android.R.color.transparent
                        )
                    )
                }

                row_root.setOnClickListener {
                    onRowSelected(position)
                    notifyDataSetChanged()
                }
            }
        }
    }

    enum class DetailItemModel(
        val title: String,
        val detail: String,
        val backgroundColor: Int
    ) {
        ONE(
            "Item 1",
            "This is a quick demo of master/detail flow using Director. In portrait mode you'll see a standard list. In landscape, you'll see a two-pane layout.",
            R.color.green_300
        ),
        TWO("Item 2", "This is another item.", R.color.cyan_300),
        THREE("Item 3", "Wow, a 3rd item!", R.color.deep_purple_300)
    }

    companion object {
        private const val KEY_SELECTED_INDEX = "MasterDetailListController.selectedIndex"
    }
}