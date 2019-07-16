package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ivianuu.director.hasRoot
import com.ivianuu.director.popTop
import com.ivianuu.director.router
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction

class MainActivity : AppCompatActivity(), ToolbarProvider {

    override val toolbar: Toolbar?
        get() = findViewById(R.id.toolbar)

    private val router by lazy { router(R.id.controller_container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        INSTANCE = this
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (!router.hasRoot) {
            router.setRoot(HomeController().toTransaction())
        }

        toolbar!!.setNavigationOnClickListener { router.popTop() }
    }

    override fun onDestroy() {
        INSTANCE = null
        super.onDestroy()
    }

    companion object {
        var INSTANCE: MainActivity? = null
            private set
    }
}

fun mainActivity(): MainActivity = MainActivity.INSTANCE!!