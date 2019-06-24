package com.ivianuu.director.sample

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.ivianuu.director.Router
import com.ivianuu.director.RouterManager
import com.ivianuu.director.backstackSize
import com.ivianuu.director.doOnChangeStarted
import com.ivianuu.director.getRouter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.popTop
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.sample.util.LoggingControllerLifecycleListener
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.activity_main.*

class RouterManagerHolder : ViewModel() {
    val routerManager = RouterManager()
}

class MainActivity : AppCompatActivity(), ToolbarProvider {

    override val toolbar: Toolbar?
        get() = findViewById(R.id.toolbar)

    private val routerManager by lazy {
        ViewModelProviders.of(this)[RouterManagerHolder::class.java].routerManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        INSTANCE = this
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        with(routerManager.getRouter(controller_container)) {
            if (!hasRoot) {
                addControllerLifecycleListener(
                    LoggingControllerLifecycleListener(),
                    recursive = true
                )
                addToolbarHandling()
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
        INSTANCE = null
        if (!isChangingConfigurations) {
            routerManager.onDestroy()
        } else {
            routerManager.removeRootView()
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (!routerManager.handleBack()) {
            super.onBackPressed()
        }
    }

    companion object {
        var INSTANCE: MainActivity? = null
            private set
    }
}

fun mainActivity(): MainActivity = MainActivity.INSTANCE!!

private fun Router.addToolbarHandling() {
    fun updateToolbarVisibility() {
        TransitionManager.beginDelayedTransition(
            mainActivity().toolbar,
            AutoTransition().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                duration = 180
            }
        )

        mainActivity().toolbar!!.navigationIcon = if (backstackSize > 1) {
            mainActivity().getDrawable(R.drawable.abc_ic_ab_back_material)
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

    mainActivity().toolbar!!.setNavigationOnClickListener { popTop() }

    updateToolbarVisibility()
}