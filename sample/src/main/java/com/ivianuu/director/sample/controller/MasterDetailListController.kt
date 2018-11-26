package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.pushController
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.buildModels
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_master_detail_list.detail_container
import kotlinx.android.synthetic.main.controller_master_detail_list.recycler_view
import kotlinx.android.synthetic.main.row_detail_item.item_title
import kotlinx.android.synthetic.main.row_detail_item.row_root

class MasterDetailListController : BaseController() {

    private var selectedIndex = 0
    private var twoPaneView = false

    override val layoutRes get() = R.layout.controller_master_detail_list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Master/Detail Flow"
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.buildModels {
            DetailItem.values().forEachIndexed { index, item ->
                detailItem {
                    id(index)
                    item(item)
                    index(index)
                    selectedIndex(selectedIndex)
                    onClick { onItemClicked(item, index) }
                }
            }
        }

        twoPaneView = detail_container != null

        if (twoPaneView) {
            onItemClicked(DetailItem.values()[selectedIndex], selectedIndex)
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

    private fun onItemClicked(item: DetailItem, index: Int) {
        selectedIndex = index

        val controller = ChildController.newInstance(item.detail, item.backgroundColor, true)

        if (twoPaneView) {
            @Suppress("PLUGIN_WARNING")
            getChildRouter(detail_container).setRoot(controller.toTransaction())
        } else {
            router.pushController(
                controller.toTransaction()
                    .pushChangeHandler(HorizontalChangeHandler())
                    .popChangeHandler(HorizontalChangeHandler())
            )
        }
    }

    companion object {
        private const val KEY_SELECTED_INDEX = "MasterDetailListController.selectedIndex"
    }
}

enum class DetailItem(
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

@EpoxyModelClass(layout = R.layout.row_detail_item)
abstract class DetailItemModel : BaseEpoxyModel() {

    @EpoxyAttribute lateinit var item: DetailItem
    @EpoxyAttribute var index: Int = 0
    @EpoxyAttribute var twoPaneView: Boolean = false
    @EpoxyAttribute var selectedIndex: Int = 0

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            item_title.text = item.title
            if (twoPaneView && index == selectedIndex) {
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
        }
    }
}