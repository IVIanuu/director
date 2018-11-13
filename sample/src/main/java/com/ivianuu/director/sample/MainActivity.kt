package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.Router
import com.ivianuu.director.attachRouter
import com.ivianuu.director.sample.controller.HomeController
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

        router = attachRouter(controller_container, null)

        router.controllerFactory = LoggingControllerFactory()

        Router.extractRouterState(controller_container.id, savedInstanceState)
            ?.let {
                router.restoreInstanceState(it)
                router.rebindIfNeeded()
            }

        if (!router.hasRootController) {
            router.setRoot(HomeController().toTransaction())
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }
}