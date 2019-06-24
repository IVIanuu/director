package com.ivianuu.director.sample.controller

import android.graphics.PorterDuff.Mode
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.ivianuu.director.changeHandler
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.common.show
import com.ivianuu.director.push
import com.ivianuu.director.requireView

import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ArcFadeMoveChangeHandler
import com.ivianuu.director.sample.mainActivity
import com.ivianuu.director.sample.util.BaseEpoxyModel
import com.ivianuu.director.sample.util.buildModels
import com.ivianuu.director.tag
import com.ivianuu.director.toTransaction
import com.ivianuu.epoxyktx.KtEpoxyHolder
import kotlinx.android.synthetic.main.controller_home.*
import kotlinx.android.synthetic.main.row_home.*

class HomeController : BaseController() {

    override val layoutRes get() = R.layout.controller_home
    override val toolbarTitle: String?
        get() = "Director Sample"

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        recycler_view.layoutManager = LinearLayoutManager(mainActivity())
        recycler_view.buildModels {
            HomeItem.values().forEachIndexed { index, item ->
                homeItem {
                    id(index)
                    item(item)
                    position(index)
                    onClick { onItemClicked(item, index) }
                }
            }
        }
    }

    private fun onItemClicked(item: HomeItem, position: Int) {
        when (item) {
            HomeItem.NAVIGATION -> {
                router.push(
                    NavigationController(
                        0,
                        NavigationController.DisplayUpMode.SHOW_FOR_CHILDREN_ONLY, false
                    )
                        .toTransaction()
                        .tag(NavigationController.TAG_UP_TRANSACTION)
                )
            }
            HomeItem.TRANSITIONS -> {
                router.push(
                    TransitionDemoController
                        .getNextTransaction(0, this)
                )
            }
            HomeItem.TARGET_CONTROLLER -> {
                router.push(TargetDisplayController().toTransaction())
            }
            HomeItem.VIEW_PAGER -> {
                router.push(PagerController().toTransaction())
            }
            HomeItem.BOTTOM_NAV -> {
                router.push(BottomNavController().toTransaction())
            }
            HomeItem.CHILD_CONTROLLERS -> {
                router.push(ParentController().toTransaction())
            }
            HomeItem.SHARED_ELEMENT_TRANSITIONS -> {
                val titleSharedElementName =
                    requireView().resources.getString(
                        R.string.transition_tag_title_indexed,
                        position
                    )
                val dotSharedElementName =
                    requireView().resources.getString(R.string.transition_tag_dot_indexed, position)

                router.push(
                    CityGridController(item.title, item.color, position)
                        .toTransaction()
                        .changeHandler(
                            ArcFadeMoveChangeHandler(
                                listOf(
                                    titleSharedElementName,
                                    dotSharedElementName
                                )
                            )
                        )
                )
            }
            HomeItem.DRAG_DISMISS -> {
                router.push(
                    DragDismissController()
                        .toTransaction()
                        .changeHandler(FadeChangeHandler(removesFromViewOnPush = false))
                )
            }
            HomeItem.MULTIPLE_CHILD_ROUTERS -> {
                router.push(MultipleChildRouterController().toTransaction())
            }
            HomeItem.MASTER_DETAIL -> {
                router.push(MasterDetailListController().toTransaction())
            }
            HomeItem.DIALOG -> {
                SimpleDialogController().show(router)
            }
            HomeItem.EXTERNAL_MODULES -> {
                router.push(ExternalModulesController().toTransaction())
            }
        }
    }
}

@EpoxyModelClass(layout = R.layout.row_home)
abstract class HomeItemModel : BaseEpoxyModel() {

    @EpoxyAttribute
    lateinit var item: HomeItem
    @EpoxyAttribute
    var position: Int = -1

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)
        with(holder) {
            home_title.text = item.title
            home_image.drawable.setColorFilter(
                ContextCompat.getColor(containerView.context, item.color),
                Mode.SRC_ATOP
            )

            home_title.transitionName =
                containerView.resources.getString(
                    R.string.transition_tag_title_indexed,
                    position
                )

            home_image.transitionName =
                containerView.resources.getString(R.string.transition_tag_dot_indexed, position)
        }
    }
}

enum class HomeItem(val title: String, val color: Int) {
    NAVIGATION("Navigation Demos", R.color.red_300),
    TRANSITIONS("Transition Demos", R.color.blue_grey_300),
    SHARED_ELEMENT_TRANSITIONS("Shared Element Demos", R.color.purple_300),
    CHILD_CONTROLLERS("Child Controllers", R.color.orange_300),
    VIEW_PAGER("ViewPager", R.color.green_300),
    BOTTOM_NAV("Bottom Nav", R.color.blue_300),
    TARGET_CONTROLLER("Target Controller", R.color.pink_300),
    MULTIPLE_CHILD_ROUTERS("Multiple Child Routers", R.color.deep_orange_300),
    MASTER_DETAIL("Master Detail", R.color.grey_300),
    DRAG_DISMISS("Drag Dismiss", R.color.lime_300),
    DIALOG("Dialog", R.color.blue_300),
    EXTERNAL_MODULES("Bonus Modules", R.color.teal_300);
}
