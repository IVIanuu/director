package com.ivianuu.director.sample.controller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.director.activitycallbacks.addActivityResultListener
import com.ivianuu.director.activitycallbacks.startActivityForResult
import com.ivianuu.director.changeHandler
import com.ivianuu.director.common.changehandler.HorizontalChangeHandler

import com.ivianuu.director.push
import com.ivianuu.director.sample.R
import com.ivianuu.director.toTransaction
import kotlinx.android.synthetic.main.controller_target_display.btn_pick_image
import kotlinx.android.synthetic.main.controller_target_display.btn_pick_title
import kotlinx.android.synthetic.main.controller_target_display.image_view
import kotlinx.android.synthetic.main.controller_target_display.tv_selection

class TargetDisplayController : BaseController(),
    TargetTitleEntryController.TargetTitleEntryControllerListener {

    override val layoutRes get() = R.layout.controller_target_display

    private var selectedText: String? = null
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbarTitle = "Target Controller Demo"

        if (savedInstanceState != null) {
            selectedText = savedInstanceState.getString(KEY_SELECTED_TEXT)

            val uriString = savedInstanceState.getString(KEY_SELECTED_IMAGE, "")
            if (uriString.isNotEmpty()) {
                imageUri = Uri.parse(uriString)
            }
        }

        addActivityResultListener(REQUEST_SELECT_IMAGE) { requestCode, resultCode, data ->
            if (requestCode == REQUEST_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
                imageUri = data?.data
                setImageView()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedViewState).apply {
            btn_pick_title.setOnClickListener {
                router.push(
                    TargetTitleEntryController.newInstance(this@TargetDisplayController)
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

            setTextView()
            setImageView()
        }
    }

    override fun onTitlePicked(option: String) {
        selectedText = option
        setTextView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED_TEXT, selectedText)
        outState.putString(
            KEY_SELECTED_IMAGE,
            if (imageUri != null) imageUri!!.toString() else null
        )
    }

    private fun setImageView() {
        image_view.setImageURI(imageUri)
    }

    private fun setTextView() {
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
        private const val KEY_SELECTED_TEXT = "TargetDisplayController.selectedText"
        private const val KEY_SELECTED_IMAGE = "TargetDisplayController.selectedImage"
    }
}