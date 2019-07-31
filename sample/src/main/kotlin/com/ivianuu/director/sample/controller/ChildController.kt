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

import android.view.View
import androidx.core.content.ContextCompat
import com.ivianuu.director.requireActivity
import com.ivianuu.director.sample.R
import kotlinx.android.synthetic.main.controller_child.*

class ChildController(
    private val title: String,
    private val bgColor: Int,
    private val colorIsRes: Boolean
) : BaseController() {

    override val layoutRes get() = R.layout.controller_child

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        tv_title.text = title

        var bgColor = this.bgColor
        if (colorIsRes) {
            bgColor = ContextCompat.getColor(requireActivity(), bgColor)
        }

        view.setBackgroundColor(bgColor)
    }

}