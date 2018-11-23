package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.View
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_multiple_child_routers.container_0
import kotlinx.android.synthetic.main.controller_multiple_child_routers.container_1
import kotlinx.android.synthetic.main.controller_multiple_child_routers.container_2

class MultipleChildRouterController : BaseController() {

    override val layoutRes get() = R.layout.controller_multiple_child_routers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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