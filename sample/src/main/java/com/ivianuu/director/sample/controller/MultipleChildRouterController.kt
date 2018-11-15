package com.ivianuu.director.sample.controller

import android.view.View
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_multiple_child_routers.*

class MultipleChildRouterController : BaseController() {

    override val layoutRes get() = R.layout.controller_multiple_child_routers

    override fun onCreate() {
        super.onCreate()
        actionBarTitle = "Child Router Demo"
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        listOf(container_0, container_1, container_2)
            .map { getChildRouter(it) }
            .filterNot { it.hasRootController }
            .forEach {
                it.setRoot(
                    NavigationController.newInstance(
                        0,
                        NavigationController.DisplayUpMode.HIDE,
                        false
                    )
                        .toTransaction()
                )
            }
    }
}