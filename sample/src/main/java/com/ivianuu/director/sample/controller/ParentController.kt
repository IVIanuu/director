package com.ivianuu.director.sample.controller

import android.os.Bundle
import android.view.ViewGroup
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.ControllerChangeType
import com.ivianuu.director.activity
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.doOnChangeEnd
import com.ivianuu.director.getChildRouter
import com.ivianuu.director.hasRootController
import com.ivianuu.director.pop
import com.ivianuu.director.resources
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.ColorUtil
import com.ivianuu.director.setRoot

class ParentController : BaseController() {

    private var finishing = false
    private var hasShownAll = false

    override val layoutRes get() = R.layout.controller_parent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarTitle = "Parent/Child Demo"
    }

    override fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(changeHandler, changeType)

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

            if (!childRouter.hasRootController) {
                val childController = ChildController.newInstance(
                    "Child Controller #$index",
                    ColorUtil.getMaterialColor(resources, index),
                    false
                )

                childController.doOnChangeEnd { _, _, changeType ->
                    if (!isBeingDestroyed) {
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
                                router.pop(this@ParentController)
                            }
                        }
                    }
                }

                childRouter.setRoot(childController, FadeChangeHandler(), FadeChangeHandler())
            }
        }
    }

    private fun removeChild(index: Int) {
        val childRouters = childRouters
        if (index < childRouters.size) {
            removeChildRouter(childRouters[index])
        }
    }

    override fun handleBack(): Boolean {
        val childControllers = childRouters.count { it.hasRootController }

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
