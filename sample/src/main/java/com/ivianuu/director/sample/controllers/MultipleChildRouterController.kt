package com.ivianuu.director.sample.controllers

import android.view.View
import com.ivianuu.director.common.FadeChangeHandler
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_multiple_child_routers.*

class MultipleChildRouterController : BaseController() {

    override var title: String?
        get() = "Child Router Demo"
        set(value) { super.title = value }

    override val layoutRes = R.layout.controller_multiple_child_routers

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        listOf(container_0, container_1, container_2)
            .map { getChildRouter(it) }
            .filterNot { it.hasRootController }
            .forEach {
                it.setRoot(
                    NavigationDemoController.newInstance(0, NavigationDemoController.DisplayUpMode.HIDE)
                        .toTransaction()
                )
            }

        show_top.setOnClickListener {
            router.pushController(
                TextController.newInstance("Dummy")
                    .toTransaction()
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
            )
        }
    }
}