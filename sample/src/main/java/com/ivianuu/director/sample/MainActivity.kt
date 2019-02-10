package com.ivianuu.director.sample

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.director.changeHandler
import com.ivianuu.director.common.changehandler.FadeChangeHandler
import com.ivianuu.director.fragmenthost.getRouter
import com.ivianuu.director.hasRoot
import com.ivianuu.director.push
import com.ivianuu.director.sample.controller.DragDismissController
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
                push(
                    DragDismissController().toTransaction()
                        .changeHandler(FadeChangeHandler(removesFromViewOnPush = true))
                )
                push(
                    DragDismissController().toTransaction()
                        .changeHandler(FadeChangeHandler(removesFromViewOnPush = false))
                )

                val handler = Handler()

                handler.postDelayed(Runnable {
                    setBackstack(
                        listOf(
                            backstack[0],
                            backstack[1].controller.toTransaction().changeHandler(
                                FadeChangeHandler(removesFromViewOnPush = false)
                            ),
                            backstack[2]
                        ), true
                    )
                }, 2500)
            }
        }
    }

}