package com.ivianuu.director.sample.test

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.Controller
import com.ivianuu.director.popCurrentController

/**
 * @author Manuel Wrage (IVIanuu)
 */
class InstructionsController : Controller() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionGranted = true
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return View(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            setBackgroundColor(Color.RED)

            setOnClickListener { router.popCurrentController() }
        }
    }

}