package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.Router
import com.ivianuu.director.fragmenthost.getRouter
import com.ivianuu.director.handleBack
import com.ivianuu.director.hasRootController
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.sample.util.LoggingChangeListener
import com.ivianuu.director.sample.util.LoggingControllerFactory
import com.ivianuu.director.sample.util.LoggingLifecycleListener
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.activity_main.toolbar

class MainActivity : AppCompatActivity(), ActionBarProvider {

    private lateinit var router: Router

    override val providedActionBar: ActionBar
        get() = supportActionBar!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        router = getRouter(
            R.id.controller_container,
            LoggingControllerFactory()
        ).apply {
            addLifecycleListener(LoggingLifecycleListener(), true)
            addChangeListener(LoggingChangeListener(), true)

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