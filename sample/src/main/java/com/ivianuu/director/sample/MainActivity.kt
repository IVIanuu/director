package com.ivianuu.director.sample

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.Router
import com.ivianuu.director.attachRouter
import com.ivianuu.director.d
import com.ivianuu.director.handleBack
import com.ivianuu.director.hasRootController
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.sample.util.LoggingControllerFactory
import com.ivianuu.director.sample.util.LoggingLifecycleListener
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.activity_main.controller_container
import kotlinx.android.synthetic.main.activity_main.toolbar

class MainActivity : AppCompatActivity(), ActionBarProvider {

    private lateinit var router: Router

    override val providedActionBar: ActionBar
        get() = supportActionBar!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        controller_container.setOnHierarchyChangeListener(object :
            ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                this@MainActivity.d { "on child view added ${child.javaClass.simpleName}, ${controller_container.childCount}" }
            }

            override fun onChildViewRemoved(parent: View, child: View) {
                this@MainActivity.d { "on child view removed ${child.javaClass.simpleName}, ${controller_container.childCount}" }
            }
        })

        router = attachRouter(
            controller_container,
            savedInstanceState,
            LoggingControllerFactory()
        ) {
            addLifecycleListener(LoggingLifecycleListener())

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