package com.ivianuu.director.sample

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.Router
import com.ivianuu.director.fragmenthost.getOrCreateRouter
import com.ivianuu.director.sample.controller.HomeController
import com.ivianuu.director.setRootIfEmpty
import kotlinx.android.synthetic.main.activity_main.toolbar

class MainActivity : AppCompatActivity(), ActionBarProvider {

    private lateinit var router: Router

    override val providedActionBar: ActionBar
        get() = supportActionBar!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        router = getOrCreateRouter(R.id.controller_container).apply {
            setRootIfEmpty {
                controller(HomeController())
            }
        }
    }

}