package com.ivianuu.director.sample.controller

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.ivianuu.director.Controller
import com.ivianuu.director.getControllerByInstanceId
import com.ivianuu.director.popTop
import com.ivianuu.director.routerManager
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.bundleOf
import kotlinx.android.synthetic.main.controller_target_title_entry.btn_use_title
import kotlinx.android.synthetic.main.controller_target_title_entry.edit_text

class TargetTitleEntryController : BaseController() {

    override val layoutRes get() = R.layout.controller_target_title_entry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbarTitle = "Target Controller Demo"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedViewState).apply {
            btn_use_title.setOnClickListener {
                val targetInstanceId = args.getString(KEY_TARGET_INSTANCE_ID)!!
                val targetController = routerManager.getControllerByInstanceId(targetInstanceId)
                (targetController as? TargetTitleEntryControllerListener)
                    ?.onTitlePicked(edit_text.text.toString())
                router.popTop()
            }
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        val imm =
            edit_text.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edit_text!!.windowToken, 0)
    }

    interface TargetTitleEntryControllerListener {
        fun onTitlePicked(option: String)
    }

    companion object {
        private const val KEY_TARGET_INSTANCE_ID = "TargetTitleEntryController.targetInstanceId"

        fun <T> newInstance(targetController: T)
                where T : Controller, T : TargetTitleEntryControllerListener = TargetTitleEntryController().apply {
            args = bundleOf(KEY_TARGET_INSTANCE_ID to targetController.instanceId)
        }

    }
}