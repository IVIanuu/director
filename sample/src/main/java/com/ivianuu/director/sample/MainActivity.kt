package com.ivianuu.director.sample

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ivianuu.director.*
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.sample.util.LoggingControllerLifecycleListener
import kotlinx.android.synthetic.main.activity_main.controller_container

class MainActivity : AppCompatActivity(), ToolbarProvider {

    override val toolbar: Toolbar?
        get() = findViewById(R.id.toolbar)

    private val routerManager = RouterManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        routerManager.postponeFullRestore()
        routerManager.restoreInstanceState(savedInstanceState)

        with(routerManager.getRouter(controller_container)) {
            addControllerLifecycleListener(LoggingControllerLifecycleListener(), recursive = true)

            addToolbarHandling()

            routerManager.startPostponedFullRestore()

            if (!hasRoot) {
                setRoot(HomeController().toTransaction())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        routerManager.onStart()
    }

    override fun onStop() {
        routerManager.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        routerManager.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        routerManager.saveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (!routerManager.handleBack()) {
            super.onBackPressed()
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