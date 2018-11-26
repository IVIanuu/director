package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ivianuu.director.Router
import com.ivianuu.director.attachRouter
import com.ivianuu.director.handleBack
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.sample.util.LoggingControllerFactory
import com.ivianuu.director.sample.util.LoggingLifecycleListener
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.activity_main.controller_container
import kotlinx.android.synthetic.main.activity_main.toolbar

class MyFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
        requireFragmentManager().beginTransaction()
            .remove(this)
            .commitNow()
    }

}

class MainActivity : AppCompatActivity(), ActionBarProvider {

    private lateinit var router: Router

    override val providedActionBar: ActionBar
        get() = supportActionBar!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        supportFragmentManager.beginTransaction()
            .add(MyFragment(), "tag")
        //   .commit()

        router = attachRouter(
            controller_container,
            savedInstanceState,
            LoggingControllerFactory()
        ).apply {
            addLifecycleListener(LoggingLifecycleListener())

            if (!hasRootController) {
                setRoot(HomeController().toTransaction())
            }

//            val nextController = TextController.newInstance("hallo")
//            pushController(nextController.toTransaction())
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

}