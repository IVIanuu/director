package com.ivianuu.director.sample.controller

import android.os.Bundle
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRootIfEmpty

class MultipleChildRouterController : BaseController() {

    override val layoutRes get() = R.layout.controller_multiple_child_routers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Child Router Demo"

        listOf(R.id.container_0, R.id.container_1, R.id.container_2)
            .map { getChildRouter(it) }
            .forEach {
                it.setRootIfEmpty {
                    controller(
                        NavigationController.newInstance(
                            0,
                            NavigationController.DisplayUpMode.HIDE,
                            false
                        )
                    )
                }
            }
    }

}