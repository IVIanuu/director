package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.fragmenthost.getRouter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.setRoot
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.activity_main.controller_container
import kotlinx.android.synthetic.main.activity_main.toolbar

class MainActivity : AppCompatActivity(), ActionBarProvider {

    override val providedActionBar: ActionBar
        get() = supportActionBar!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        with(getRouter(controller_container)) {
            if (!hasRoot) {
                setRoot(HomeController().toTransaction())
            }
        }
    }

}