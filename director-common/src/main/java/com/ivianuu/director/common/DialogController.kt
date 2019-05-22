/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director.common

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.*

/**
 * A controller counterpart for dialog fragments
 */
abstract class DialogController : Controller(), DialogInterface.OnCancelListener,
    DialogInterface.OnDismissListener {

    /**
     * The dialog of this controller
     */
    var dialog: Dialog? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = View(inflater.context) // dummy view

        val dialog = onCreateDialog(savedViewState).also { this.dialog = it }

        dialog.setOnCancelListener(this)
        dialog.setOnDismissListener(this)

        savedViewState?.getBundle(KEY_DIALOG_STATE)?.let { dialog.onRestoreInstanceState(it) }

        return view
    }

    /**
     * Creates dialog which should be shown in this controller
     */
    protected abstract fun onCreateDialog(savedViewState: Bundle?): Dialog

    override fun onAttach(view: View) {
        super.onAttach(view)
        dialog?.show()
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        dialog?.hide()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        dialog?.dismiss()
        dialog = null
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        dialog?.onSaveInstanceState()?.let { outState.putBundle(KEY_DIALOG_STATE, it) }
    }

    override fun onCancel(dialog: DialogInterface) {
    }

    override fun onDismiss(dialog: DialogInterface) {
        dismiss()
    }

    /**
     * Dismiss the dialog and pop this controller
     */
    open fun dismiss() {
        dialog?.dismiss()
        router.pop(this)
    }

    companion object {
        private const val KEY_DIALOG_STATE = "DialogController.dialogState"
    }
}

fun DialogController.requireDialog(): Dialog = dialog ?: error("dialog == null")

fun DialogController.show(router: Router, tag: String? = null) {
    router.push(
        changeHandler(DefaultChangeHandler(removesFromViewOnPush = false)).tag(tag)
    )
}