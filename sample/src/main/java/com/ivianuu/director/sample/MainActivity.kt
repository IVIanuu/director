package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.Controller
import com.ivianuu.director.Router
import com.ivianuu.director.attachRouter
import com.ivianuu.director.contributor.HasControllerInjector
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.toTransaction
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : AppCompatActivity(), ActionBarProvider, HasControllerInjector {

    @Inject lateinit var controllerInjector: DispatchingAndroidInjector<Controller>

    private lateinit var router: Router

    override val providedActionBar: ActionBar
        get() = supportActionBar!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        router = attachRouter(
            controller_container,
            savedInstanceState, LoggingControllerFactory()
        ).apply {
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

    override fun controllerInjector(): AndroidInjector<Controller> = controllerInjector
}