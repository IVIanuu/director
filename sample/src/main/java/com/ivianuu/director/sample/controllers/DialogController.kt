package com.ivianuu.director.sample.controllers

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import com.ivianuu.director.requireRouter
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.BundleBuilder
import kotlinx.android.synthetic.main.controller_dialog.*

class DialogController : BaseController() {

    override val layoutRes = R.layout.controller_dialog

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        tv_title.text = args.getString(KEY_TITLE)
        tv_description.text = args.getString(KEY_DESCRIPTION)
        tv_description.movementMethod = LinkMovementMethod.getInstance()

        dismiss.setOnClickListener { requireRouter().popController(this) }
        dialog_window.setOnClickListener { requireRouter().popController(this) }
    }

    companion object {
        private const val KEY_TITLE = "DialogController.title"
        private const val KEY_DESCRIPTION = "DialogController.description"

        fun newInstance(title: String, description: String) = DialogController().apply {
            args = BundleBuilder(Bundle())
                .putCharSequence(KEY_TITLE, title)
                .putCharSequence(KEY_DESCRIPTION, description)
                .build()
        }
    }
}