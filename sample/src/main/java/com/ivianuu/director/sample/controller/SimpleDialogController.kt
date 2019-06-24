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

package com.ivianuu.director.sample.controller

import android.app.Dialog
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.ivianuu.director.common.DialogController
import com.ivianuu.director.sample.mainActivity

/**
 * @author Manuel Wrage (IVIanuu)
 */
class SimpleDialogController : DialogController() {

    override fun onCreateDialog(): Dialog = MaterialDialog.Builder(mainActivity())
        .title("Hello")
        .content("This is a simple dialog controller.")
        .positiveText("OK")
        .onPositive { _, _ ->
            Toast.makeText(mainActivity(), "Ok clicked!", Toast.LENGTH_SHORT).show()
        }
        .negativeText("Cancel")
        .onNegative { _, _ -> dismiss() }
        .build()

}