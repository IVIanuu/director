package com.ivianuu.director.sample.controller

import android.content.res.Configuration
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.childRouter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.popTop
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.simpleController
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_master_detail_list.*
import kotlinx.android.synthetic.main.row_detail_item.*

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

    private val childRouter by lazy { childRouter(R.id.detail_container) }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        recycler_view.layoutManager = LinearLayoutManager(requireActivity())
        recycler_view.adapter = epoxyController.adapter
        isTwoPane = view.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE

        childRouter.popsLastView = !isTwoPane

        epoxyController.requestModelBuild()

        if (isTwoPane && !childRouter.hasRoot) {
            onItemClicked(DetailItem.values()[selectedIndex], selectedIndex)
        }
    }

    override fun handleBack(): Boolean {
        return if (childRouter.hasRoot) {
            childRouter.popTop()
            true
        } else {
            false
        }
    }

    private fun onItemClicked(item: DetailItem, index: Int) {
        selectedIndex = index
        childRouter.setRoot(
            ChildController(
                item.detail, item.backgroundColor, true
            ).toTransaction()
        )
        epoxyController.requestModelBuild()
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

    @EpoxyAttribute
    lateinit var item: DetailItem
    @EpoxyAttribute
    var index: Int = 0
    @EpoxyAttribute
    var twoPane: Boolean = false
    @EpoxyAttribute
    var selectedIndex: Int = 0

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