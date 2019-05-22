package com.ivianuu.director.sample

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ivianuu.director.*
import com.ivianuu.director.fragment.getRouter
import com.ivianuu.director.fragment.postponeFullRestore
import com.ivianuu.director.fragment.startPostponedFullRestore
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.sample.util.LoggingControllerLifecycleListener
import kotlinx.android.synthetic.main.activity_main.controller_container

class MainActivity : AppCompatActivity(), ToolbarProvider {

    override val toolbar: Toolbar?
        get() = findViewById(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        postponeFullRestore()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        with(getRouter(controller_container)) {
            addControllerLifecycleListener(LoggingControllerLifecycleListener(), recursive = true)

            addToolbarHandling()

            startPostponedFullRestore()

            if (!hasRoot) {
                setRoot(HomeController())
            }
        }
    }

    private fun Router.addToolbarHandling() {
        fun updateToolbarVisibility() {
            TransitionManager.beginDelayedTransition(
                toolbar,
                AutoTransition().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    duration = 180
                }
            )

            toolbar!!.navigationIcon = if (backstackSize > 1) {
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

        doOnChangeStarted { _, _, _, _, _, _ -> updateToolbarVisibility() }

        toolbar!!.setNavigationOnClickListener { popTop() }

        updateToolbarVisibility()
    }
}