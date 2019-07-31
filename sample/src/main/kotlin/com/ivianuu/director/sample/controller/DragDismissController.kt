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
import com.ivianuu.director.popController
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.director.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback

class DragDismissController : BaseController() {

    override val layoutRes get() = R.layout.controller_drag_dismiss
    override val toolbarTitle: String?
        get() = "Drag to Dismiss"

    private val dragDismissListener = object : ElasticDragDismissCallback {
        override fun onDragDismissed() {
            (view as ElasticDragDismissFrameLayout).removeListener(this)
            router.popController(this@DragDismissController, ScaleFadeChangeHandler())
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
    }

    override fun onDetach(view: View) {
        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
        super.onDetach(view)
    }
}