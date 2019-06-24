package com.ivianuu.director.sample.controller

import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.changeHandler
import com.ivianuu.director.childRouters
import com.ivianuu.director.clear
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.doOnChangeEnded
import com.ivianuu.director.getChildRouter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.removeChildRouter
import com.ivianuu.director.requireView
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.mainActivity
import com.ivianuu.director.sample.util.ColorUtil
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

class ParentController : BaseController() {

    override val layoutRes get() = R.layout.controller_parent
    override val toolbarTitle: String?
        get() = "Parent/Child Demo"

    private var finishing = false
    private var hasShownAll = false

    override fun onAttach(view: View) {
        super.onAttach(view)
        addChild(0)
    }

    private fun addChild(index: Int) {
        val frameId = requireView().resources.getIdentifier(
            "child_content_" + (index + 1),
            "id",
            mainActivity().packageName
        )

        val container = view!!.findViewById<ViewGroup>(frameId)

        getChildRouter(container).let { childRouter ->
            childRouter.popsLastView = true

            if (!childRouter.hasRoot) {
                val childController = ChildController(
                    "Child Controller #$index",
                    ColorUtil.getMaterialColor(requireView().resources, index),
                    false
                )

                /*childController.doOnChangeEnd { _, _, _, changeType ->
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
                }*/ // todo

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
            var removed = false
            childRouter.doOnChangeEnded { _, _, _, _, _, _ ->
                if (!removed) {
                    removed = true
                    removeChildRouter(childRouter)
                }
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
