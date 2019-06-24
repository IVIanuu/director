package com.ivianuu.director.sample.controller

import com.ivianuu.director.getChildRouter
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

class MultipleChildRouterController : BaseController() {

    override val layoutRes get() = R.layout.controller_multiple_child_routers
    override val toolbarTitle: String?
        get() = "Child Router Demo"

    override fun onCreate() {
        super.onCreate()
        listOf(R.id.container_0, R.id.container_1, R.id.container_2)
            .map { getChildRouter(it) }
            .forEach {
                it.setRoot(
                    NavigationController(
                        0,
                        NavigationController.DisplayUpMode.HIDE,
                        false
                    ).toTransaction()
                )
            }
    }

}