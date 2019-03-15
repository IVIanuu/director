package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ivianuu.director.Router
import com.ivianuu.director.backstackSize
import com.ivianuu.director.doOnChangeStarted
import com.ivianuu.director.fragmenthost.getRouter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.popTop
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.activity_main.controller_container

class MainActivity : AppCompatActivity(), ToolbarProvider {

    override val toolbar: Toolbar?
        get() = findViewById(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        with(getRouter(controller_container)) {
            addToolbarHandling()

            if (!hasRoot) {
                setRoot(HomeController().toTransaction())
            }
        }

    }

    private fun Router.addToolbarHandling() {
        fun updateToolbarVisibility() {
            android.transition.TransitionManager.beginDelayedTransition(toolbar,
                android.transition.AutoTransition().apply {
                    ordering = android.transition.TransitionSet.ORDERING_TOGETHER
                    duration = 180
                })

            toolbar!!.navigationIcon = if (backstackSize > 1) {
                getDrawable(com.ivianuu.director.sample.R.drawable.abc_ic_ab_back_material)
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

        doOnChangeStarted { _, _, _, _, _, _ -> updateToolbarVisibility() }

        toolbar!!.setNavigationOnClickListener { popTop() }
    }
}