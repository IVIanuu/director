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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.requireActivity
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseController : Controller(), LayoutContainer {

    override val containerView: View?
        get() = _containerView

    private var _containerView: View? = null

    protected open val layoutRes = 0

    protected open val toolbarTitle: String? get() = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup
    ): View {
        check(layoutRes != 0) { "no layout res provided" }
        val view = inflater.inflate(layoutRes, container, false)
            .also { _containerView = it }

        onViewCreated(view)

        return view
    }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
    }

    override fun onDestroyView(view: View) {
        clearFindViewByIdCache()
        _containerView = null
        super.onDestroyView(view)
    }

    private fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.toolbarTitle != null) {
                return
            }
            parentController = parentController.parentController
        }

        requireActivity().toolbar?.title = toolbarTitle
    }

    protected open fun onViewCreated(view: View) {
    }

}