package com.ivianuu.director.sample.controller

import androidx.lifecycle.Lifecycle
import com.ivianuu.director.Router
import com.ivianuu.director.childRouter
import com.ivianuu.director.sample.R
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

class MultipleChildRouterController : BaseController() {

    override val layoutRes get() = R.layout.controller_multiple_child_routers
    override val toolbarTitle: String?
        get() = "Child router Demo"

    private val childRouters = mutableListOf<Router>()

    override fun onCreate() {
        super.onCreate()
        listOf(R.id.container_0, R.id.container_1, R.id.container_2)
            .map { childRouter(it) }
            .forEach {
                childRouters.add(it)

                it.setRoot(
                    NavigationController(
                        0,
                        NavigationController.DisplayUpMode.HIDE,
                        false
                    ).toTransaction()
                )
            }
    }

    override fun handleBack(): Boolean {
        return childRouters
            .flatMap { it.backstack }
            .asSequence()
            .sortedByDescending { it.transactionIndex }
            .filter { it.controller.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
            .map { it.controller.router }
            .any { it.handleBack() }
    }
}