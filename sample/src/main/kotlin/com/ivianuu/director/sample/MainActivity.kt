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

package com.ivianuu.director.sample

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ivianuu.director.ControllerChangeListener
import com.ivianuu.director.hasRoot
import com.ivianuu.director.popTop
import com.ivianuu.director.router
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

class MainActivity : AppCompatActivity(), ToolbarProvider {

    override val toolbar: Toolbar?
        get() = findViewById(R.id.toolbar)

    private val toolbarListener = ControllerChangeListener(
        onChangeStarted = { _, _, _, _, _, _ ->
            updateToolbarVisibility()
        }
    )

    private val router by lazy { router(R.id.controller_container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        router.addChangeListener(toolbarListener)

        if (!router.hasRoot) {
            router.setRoot(HomeController().toTransaction())
        }

        toolbar!!.setNavigationOnClickListener { router.popTop() }

        updateToolbarVisibility()
    }

    override fun onDestroy() {
        router.removeChangeListener(toolbarListener)
        super.onDestroy()
    }

    private fun updateToolbarVisibility() {
        TransitionManager.beginDelayedTransition(
            toolbar,
            AutoTransition().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                duration = 180
            }
        )

        toolbar!!.navigationIcon = if (router.backStack.size > 1) {
            getDrawable(R.drawable.abc_ic_ab_back_material)
                .apply {
                    setColorFilter(
                        android.graphics.Color.WHITE,
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                }
        } else {
            null
        }
    }

}