package com.ivianuu.director.sample.controller

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.ivianuu.director.popTop
import com.ivianuu.director.sample.R
import kotlinx.android.synthetic.main.controller_target_title_entry.*

class TargetTitleEntryController(
    private val onTitlePicked: (String) -> Unit
) : BaseController() {

    override val layoutRes get() = R.layout.controller_target_title_entry
    override val toolbarTitle: String?
        get() = "Target Controller Demo"

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        btn_use_title.setOnClickListener {
            onTitlePicked(edit_text.text.toString())
            router.popTop()
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        val imm =
            edit_text.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edit_text!!.windowToken, 0)
    }

    companion object {
        private const val KEY_TARGET_INSTANCE_ID = "TargetTitleEntryController.targetInstanceId"
    }
}