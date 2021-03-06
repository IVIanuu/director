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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import com.ivianuu.director.changeHandler
import com.ivianuu.director.common.addActivityResultListener
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler
import com.ivianuu.director.common.startActivityForResult
import com.ivianuu.director.push
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.d
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_target_display.*

class TargetDisplayController : BaseController() {

    override val layoutRes get() = R.layout.controller_target_display
    override val toolbarTitle: String?
        get() = "Target Controller Demo"

    private var selectedText: String? = null
    private var imageUri: Uri? = null

    override fun onCreate() {
        super.onCreate()

        addActivityResultListener(REQUEST_SELECT_IMAGE) { requestCode, resultCode, data ->
            d { "on activity result $requestCode, $resultCode, $data" }
            if (requestCode == REQUEST_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
                imageUri = data?.data
                updateImageView()
            }
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        btn_pick_title.setOnClickListener {
            router.push(
                TargetTitleEntryController {
                    selectedText = it
                    updateTextView()
                }
                    .toTransaction()
                    .changeHandler(HorizontalChangeHandler())
            )
        }

        btn_pick_image.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, "Select Image"),
                REQUEST_SELECT_IMAGE
            )
        }

        updateTextView()
        updateImageView()
    }

    private fun updateImageView() {
        image_view.setImageURI(imageUri)
    }

    private fun updateTextView() {
        if (tv_selection != null) {
            if (selectedText != null && selectedText!!.isNotEmpty()) {
                tv_selection.text = selectedText
            } else {
                tv_selection.text =
                    "Press pick title to set this title, or pick image to fill in the image view."
            }
        }
    }

    companion object {
        private const val REQUEST_SELECT_IMAGE = 126
    }
}