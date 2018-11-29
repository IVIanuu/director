package com.ivianuu.director.sample.test

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.ivianuu.director.common.changehandler.VerticalChangeHandler
import com.ivianuu.director.dialog.DialogController
import com.ivianuu.director.popChangeHandler
import com.ivianuu.director.popCurrentController
import com.ivianuu.director.pushChangeHandler
import com.ivianuu.director.pushController
import com.ivianuu.director.toTransaction

var permissionGranted = false

/**
 * @author Manuel Wrage (IVIanuu)
 */
class DialogController : DialogController() {

    override fun toString() = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionGranted = false
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog.Builder(activity)
            .title("Hello")
            .content("This is the content")
            .positiveText("Next")
            .autoDismiss(false)
            .onPositive { _, _ ->
                router.pushController(
                    InstructionsController().toTransaction()
                        .pushChangeHandler(VerticalChangeHandler())
                        .popChangeHandler(VerticalChangeHandler())
                )
            }
            .build()
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        if (permissionGranted) {
            router.popCurrentController()
        }
    }
}