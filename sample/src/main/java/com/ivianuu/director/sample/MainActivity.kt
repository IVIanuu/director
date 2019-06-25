package com.ivianuu.director.sample

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ivianuu.director.ControllerChangeListener
import com.ivianuu.director.backstackSize
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
        INSTANCE = this
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
        INSTANCE = null
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

        toolbar!!.navigationIcon = if (router.backstackSize > 1) {
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

    companion object {
        var INSTANCE: MainActivity? = null
            private set
    }
}

fun mainActivity(): MainActivity = MainActivity.INSTANCE!!