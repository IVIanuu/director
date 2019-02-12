package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ivianuu.director.fragmenthost.getRouter
import com.ivianuu.director.hasRoot
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
            if (!hasRoot) {
                setRoot(HomeController().toTransaction())
            }
        }

    }

}