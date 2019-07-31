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
import com.ivianuu.director.sample.R
import kotlinx.android.synthetic.main.controller_text.*

class TextController(private val text: String) : BaseController() {

    override val layoutRes get() = R.layout.controller_text

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        text_view.text = text
    }

}
