package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerLifecycleListener
import com.ivianuu.director.Router
import com.ivianuu.director.attachRouter
import com.ivianuu.director.internal.d
import com.ivianuu.director.sample.controllers.HomeController
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ActionBarProvider {

    private lateinit var router: Router

    override val providedActionBar: ActionBar
        get() = supportActionBar!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        router = attachRouter(controller_container, savedInstanceState).apply {
            addLifecycleListener(object : ControllerLifecycleListener {
                override fun postCreate(controller: Controller) {
                    super.postCreate(controller)
                    this@MainActivity.d { "post create $controller" }
                }
            }, false)

            if (!hasRootController) {
                setRoot(HomeController().toTransaction())
            }
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }
}
