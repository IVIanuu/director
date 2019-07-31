/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director.sample.controller

import android.os.Parcelable
import android.view.View
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.RouterTransaction
import com.ivianuu.director.changeHandler
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.common.changehandler.VerticalChangeHandler
import com.ivianuu.director.popToRoot
import com.ivianuu.director.popToTag
import com.ivianuu.director.push

import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.ColorUtil
import com.ivianuu.director.toTransaction
import com.ivianuu.director.traveler.ControllerKey
import com.ivianuu.traveler.Command
import com.ivianuu.traveler.navigate
import com.ivianuu.traveler.popToRoot
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.controller_navigation.*

class NavigationController(
    private val index: Int,
    private val displayUpMode: DisplayUpMode,
    private val useTraveler: Boolean = false,
    private val animMode: AnimMode = AnimMode.HORIZONTAL
) : BaseController() {

    override val layoutRes get() = R.layout.controller_navigation
    override val toolbarTitle: String?
        get() = "Navigation Demos"

    private val travelerRouter get() = (parentController as TravelerController).travelerRouter

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        if (displayUpMode != DisplayUpMode.SHOW) {
            btn_up.visibility = View.GONE
        }

        view.setBackgroundColor(ColorUtil.getMaterialColor(view.resources, index))
        tv_title.text = view.resources.getString(R.string.navigation_title, index)

        btn_next.setOnClickListener {
            if (useTraveler) {
                travelerRouter.navigate(
                    NavigationControllerKey(
                        index + 1,
                        displayUpMode.displayUpModeForChild,
                        useTraveler,
                        animMode
                    )
                )
            } else {
                router.push(
                    NavigationController(
                        index + 1,
                        displayUpMode.displayUpModeForChild,
                        useTraveler,
                        animMode
                    )
                        .toTransaction()
                        .changeHandler(animMode.createHandler())
                )
            }
        }

        btn_up.setOnClickListener {
            if (useTraveler) {
                // not required
            } else {
                router.popToTag(TAG_UP_TRANSACTION)
            }
        }

        btn_pop_to_root.setOnClickListener {
            if (useTraveler) {
                travelerRouter.popToRoot()
            } else {
                router.popToRoot()
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        if (view == null) return
        btn_next.isEnabled = enabled
        btn_up.isEnabled = enabled
        btn_pop_to_root.isEnabled = enabled
    }

    @Parcelize
    enum class DisplayUpMode : Parcelable {
        SHOW,
        SHOW_FOR_CHILDREN_ONLY,
        HIDE;

        val displayUpModeForChild: DisplayUpMode
            get() = when (this) {
                HIDE -> HIDE
                else -> SHOW
            }
    }

    @Parcelize
    enum class AnimMode : Parcelable {
        HORIZONTAL {
            override fun createHandler(): ControllerChangeHandler =
                HorizontalChangeHandler()
        },
        VERTICAL {
            override fun createHandler(): ControllerChangeHandler =
                VerticalChangeHandler()
        };

        abstract fun createHandler(): ControllerChangeHandler
    }

    companion object {
        const val TAG_UP_TRANSACTION = "NavigationController.up"
    }

}

data class NavigationControllerKey(
    val index: Int,
    val displayUpMode: NavigationController.DisplayUpMode,
    val useTraveler: Boolean,
    val animMode: NavigationController.AnimMode
) : ControllerKey {

    override fun createController(data: Any?) = NavigationController(
        index, displayUpMode, useTraveler, animMode
    )

    override fun setupTransaction(
        command: Command,
        currentController: Controller?,
        nextController: Controller,
        transaction: RouterTransaction
    ) {
        transaction.changeHandler(animMode.createHandler())
    }

}