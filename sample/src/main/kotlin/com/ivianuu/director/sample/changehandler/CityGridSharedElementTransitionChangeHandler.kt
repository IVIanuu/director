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

package com.ivianuu.director.sample.changehandler

import android.annotation.TargetApi
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeTransform
import android.transition.Explode
import android.transition.Slide
import android.transition.Transition
import android.transition.TransitionSet
import android.view.Gravity
import com.ivianuu.director.ChangeData
import com.ivianuu.director.common.changehandler.SharedElementTransitionChangeHandler

@TargetApi(21)
class CityGridSharedElementTransitionChangeHandler(
    private val sharedElementNames: List<String>
) : SharedElementTransitionChangeHandler() {

    override fun getExitTransition(changeData: ChangeData): Transition? {
        return if (changeData.isPush) {
            Explode()
        } else {
            Slide(Gravity.BOTTOM)
        }
    }

    override fun getSharedElementTransition(changeData: ChangeData): Transition? {
        return TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeClipBounds())
            .addTransition(ChangeTransform())
    }

    override fun getEnterTransition(changeData: ChangeData): Transition? {
        return if (changeData.isPush) {
            Slide(Gravity.BOTTOM)
        } else {
            Explode()
        }
    }

    override fun configureSharedElements(changeData: ChangeData) {
        sharedElementNames.forEach {
            addSharedElement(it)
            waitOnSharedElementNamed(it)
        }
    }

    override fun copy() = CityGridSharedElementTransitionChangeHandler(sharedElementNames)

}
