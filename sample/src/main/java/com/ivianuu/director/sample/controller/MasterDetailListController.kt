package com.ivianuu.director.sample.controller

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.*
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.simpleController
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_master_detail_list.recycler_view
import kotlinx.android.synthetic.main.row_detail_item.item_title

class MasterDetailListController : BaseController() {

    override val layoutRes get() = R.layout.controller_master_detail_list
    override val toolbarTitle: String?
        get() = "Master/Detail Flow"

    private var selectedIndex = 0
    private var isTwoPane = false

    private val epoxyController = simpleController {
        DetailItem.values().forEachIndexed { index, item ->
            detailItem {
                id(index)
                item(item)
                index(index)
                selectedIndex(selectedIndex)
                twoPane(isTwoPane)
                onClick { onItemClicked(item, index) }
            }
        }
    }

    private val childRouter by childRouter(R.id.detail_container)

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = epoxyController.adapter
        isTwoPane = resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE

        childRouter.popsLastView = !isTwoPane

        epoxyController.requestModelBuild()

        if (isTwoPane && !childRouter.hasRoot) {
            onItemClicked(DetailItem.values()[selectedIndex], selectedIndex)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedIndex = savedInstanceState.getInt(KEY_SELECTED_INDEX)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_INDEX, selectedIndex)
    }

    private fun onItemClicked(item: DetailItem, index: Int) {
        selectedIndex = index
        childRouter.setRoot(
            ChildController.newInstance(
                item.detail, item.backgroundColor, true
            ).toTransaction()
        )
        epoxyController.requestModelBuild()
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
    @EpoxyAttribute var twoPane: Boolean = false
    @EpoxyAttribute var selectedIndex: Int = 0

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            item_title.text = item.title
            if (twoPane && index == selectedIndex) {
                containerView.setBackgroundColor(
                    ContextCompat.getColor(
                        containerView.context,
                        R.color.grey_400
                    )
                )
            } else {
                containerView.setBackgroundColor(0)
            }
        }
    }
}