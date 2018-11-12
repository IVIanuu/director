package com.ivianuu.director.sample.controller

import android.graphics.PorterDuff.Mode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.requireActivity
import com.ivianuu.director.requireResources
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.TestChangeHandler
import com.ivianuu.director.sample.changehandler.ArcFadeMoveChangeHandler
import com.ivianuu.director.tag
import com.ivianuu.director.toTransaction
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.controller_home.*
import kotlinx.android.synthetic.main.row_home.*

class HomeController : BaseController() {

    override var title: String?
        get() = "Director Sample"
        set(value) { super.title = value }

    enum class DemoModel(var title: String, var color: Int) {
        NAVIGATION("Navigation Demos", R.color.red_300),
        TRANSITIONS("Transition Demos", R.color.blue_grey_300),
        SHARED_ELEMENT_TRANSITIONS("Shared Element Demos", R.color.purple_300),
        CHILD_CONTROLLERS("Child Controllers", R.color.orange_300),
        VIEW_PAGER("ViewPager", R.color.green_300),
        TARGET_CONTROLLER("Target Controller", R.color.pink_300),
        MULTIPLE_CHILD_ROUTERS("Multiple Child Routers", R.color.deep_orange_300),
        MASTER_DETAIL("Master Detail", R.color.grey_300),
        DRAG_DISMISS("Drag Dismiss", R.color.lime_300),
        DIALOG("Dialog", R.color.blue_300),
        ARCH("Arch", R.color.yellow_300)
    }

    override val layoutRes = R.layout.controller_home

    override fun onCreate() {
        super.onCreate()
        hasOptionsMenu = true
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = HomeAdapter(LayoutInflater.from(context), DemoModel.values())
        }
    }

    override fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        optionsMenuHidden = !changeType.isEnter

        if (changeType.isEnter) {
            setTitle()
        }
    }

    fun onModelRowClick(model: DemoModel?, position: Int) {
        when (model) {
            HomeController.DemoModel.NAVIGATION -> {
                router.pushController(
                    NavigationDemoController.newInstance(
                        0,
                        NavigationDemoController.DisplayUpMode.SHOW_FOR_CHILDREN_ONLY)
                        .toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                        .tag(NavigationDemoController.TAG_UP_TRANSACTION)
                )
            }
            HomeController.DemoModel.TRANSITIONS -> {
                router.pushController(
                    TransitionDemoController.getRouterTransaction(
                        0,
                        this
                    )
                )
            }
            HomeController.DemoModel.TARGET_CONTROLLER -> {
                router.pushController(
                    TargetDisplayController()
                        .toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            HomeController.DemoModel.VIEW_PAGER -> {
                router.pushController(
                    PagerController().toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            HomeController.DemoModel.CHILD_CONTROLLERS -> {
                router.pushController(
                    ParentController().toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            HomeController.DemoModel.SHARED_ELEMENT_TRANSITIONS -> {
                val titleSharedElementName =
                    requireResources().getString(R.string.transition_tag_title_indexed, position)
                val dotSharedElementName =
                    requireResources().getString(R.string.transition_tag_dot_indexed, position)

                router.pushController(
                    CityGridController.newInstance(model.title, model.color, position).toTransaction()
                        .pushChangeHandler(
                            ArcFadeMoveChangeHandler(
                                titleSharedElementName,
                                dotSharedElementName
                            )
                        )
                        .popChangeHandler(
                            ArcFadeMoveChangeHandler(
                                titleSharedElementName,
                                dotSharedElementName
                            )
                        )
                )
            }
            HomeController.DemoModel.DRAG_DISMISS -> {
                router.pushController(
                    DragDismissController().toTransaction()
                        .pushChangeHandler(
                            FadeChangeHandler(
                                removesFromViewOnPush = false
                            )
                        )
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            HomeController.DemoModel.MULTIPLE_CHILD_ROUTERS -> {
                router.pushController(
                    MultipleChildRouterController()
                        .toTransaction()
                        .pushChangeHandler(TestChangeHandler())
                        .popChangeHandler(TestChangeHandler())
                )
            }
            HomeController.DemoModel.MASTER_DETAIL -> {
                router.pushController(
                    MasterDetailListController().toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            DemoModel.DIALOG -> {
                SimpleDialogController().show(router)
            }
            DemoModel.ARCH -> {
                router.pushController(
                    ArchController().toTransaction()
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
        }
    }

    private inner class HomeAdapter(
        private val inflater: LayoutInflater,
        private val items: Array<DemoModel>
    ) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(inflater.inflate(R.layout.row_home, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position, items[position])
        }

        override fun getItemCount() = items.size

        private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            LayoutContainer {

            override val containerView: View?
                get() = itemView

            private var model: DemoModel? = null

            fun bind(position: Int, item: DemoModel) {
                model = item
                tv_title.text = item.title
                img_dot.drawable.setColorFilter(
                    ContextCompat.getColor(requireActivity(), item.color),
                    Mode.SRC_ATOP
                )

                ViewCompat.setTransitionName(
                    tv_title,
                    requireResources().getString(R.string.transition_tag_title_indexed, position)
                )

                ViewCompat.setTransitionName(
                    img_dot,
                    requireResources().getString(R.string.transition_tag_dot_indexed, position)
                )

                itemView.setOnClickListener { onModelRowClick(model, position) }
            }
        }
    }
}
