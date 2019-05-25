package com.ivianuu.director.sample.controller

import android.view.ViewGroup
import com.ivianuu.director.*
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.ColorUtil

class ParentController : BaseController() {

    override val layoutRes get() = R.layout.controller_parent
    override val toolbarTitle: String?
        get() = "Parent/Child Demo"

    private var finishing = false
    private var hasShownAll = false

    override fun onChangeEnded(
        other: Controller?,
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(other, changeHandler, changeType)
        if (changeType == ControllerChangeType.PUSH_ENTER) {
            addChild(0)
        }
    }

    private fun addChild(index: Int) {
        val frameId = resources.getIdentifier(
            "child_content_" + (index + 1),
            "id",
            activity.packageName
        )

        val container = view!!.findViewById<ViewGroup>(frameId)

        getChildRouter(container).let { childRouter ->
            childRouter.popsLastView = true

            if (!childRouter.hasRoot) {
                val childController = ChildController.newInstance(
                    "Child Controller #$index",
                    ColorUtil.getMaterialColor(resources, index),
                    false
                )

                childController.doOnChangeEnd { _, _, _, changeType ->
                    if (changeType == ControllerChangeType.PUSH_ENTER && !hasShownAll) {
                        if (index < NUMBER_OF_CHILDREN - 1) {
                            addChild(index + 1)
                        } else {
                            hasShownAll = true
                        }
                    } else if (changeType == ControllerChangeType.POP_EXIT) {
                        if (index > 0) {
                            removeChild(index - 1)
                        } else {
                            router.popTop()
                        }
                    }
                }

                childRouter.setRoot(
                    childController.toTransaction()
                        .changeHandler(FadeChangeHandler())
                )
            }
        }
    }

    private fun removeChild(index: Int) {
        val childRouters = childRouters.toList()
        if (index < childRouters.size) {
            val childRouter = childRouters[index]
            childRouter.clear()
            childRouter.doOnChangeEnded { _, _, _, _, _, _ ->
                removeChildRouter(childRouter)
            }
        }
    }

    override fun handleBack(): Boolean {
        val childControllers = childRouters.count { it.hasRoot }

        return if (childControllers != NUMBER_OF_CHILDREN || finishing) {
            true
        } else {
            finishing = true
            super.handleBack()
        }
    }

    companion object {
        private const val NUMBER_OF_CHILDREN = 5
    }
}
