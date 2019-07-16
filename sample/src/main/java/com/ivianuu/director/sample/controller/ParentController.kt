package com.ivianuu.director.sample.controller

import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Router
import com.ivianuu.director.changeHandler
import com.ivianuu.director.childRouter
import com.ivianuu.director.clear
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.doOnChangeEnded
import com.ivianuu.director.hasRoot
import com.ivianuu.director.popTop
import com.ivianuu.director.requireActivity
import com.ivianuu.director.requireView
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.ColorUtil
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

class ParentController : BaseController() {

    override val layoutRes get() = R.layout.controller_parent
    override val toolbarTitle: String?
        get() = "Parent/Child Demo"

    private var finishing = false
    private var hasShownAll = false

    private val childRouters = mutableListOf<Router>()

    override fun onAttach(view: View) {
        super.onAttach(view)
        addChild(0)
    }

    private fun addChild(index: Int) {
        val frameId = requireView().resources.getIdentifier(
            "child_content_" + (index + 1),
            "id",
            requireActivity().packageName
        )

        val container = view!!.findViewById<ViewGroup>(frameId)

        childRouter(container).let { childRouter ->
            childRouters.add(childRouter)

            childRouter.popsLastView = true

            if (!childRouter.hasRoot) {
                val childController = ChildController(
                    "Child Controller #$index",
                    ColorUtil.getMaterialColor(requireView().resources, index),
                    false
                )

                childRouter.doOnChangeEnded { router, _, _, isPush, _, _ ->
                    if (isPush && !hasShownAll) {
                        if (index < NUMBER_OF_CHILDREN - 1) {
                            addChild(index + 1)
                        } else {
                            hasShownAll = true
                        }
                    } else if (!isPush) {
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
        if (index < childRouters.size) {
            val childRouter = childRouters[index]
            childRouter.clear()
            var removed = false
            childRouter.doOnChangeEnded { _, _, _, _, _, _ ->
                if (!removed) {
                    removed = true
                    childRouters.remove(childRouter)
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
