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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.ivianuu.director.activitycallbacks.addPermissionResultListener
import com.ivianuu.director.activitycallbacks.requestPermissions
import com.ivianuu.director.context
import com.ivianuu.director.hasView
import com.ivianuu.director.sample.R
import kotlinx.android.synthetic.main.controller_permission.grant
import kotlinx.android.synthetic.main.controller_permission.msg

/**
 * @author Manuel Wrage (IVIanuu)
 */
class PermissionController : BaseController() {

    override val layoutRes = R.layout.controller_permission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbarTitle = "Permission"
        addPermissionResultListener(REQUEST_CODE_PERMISSION) { _, _, _ -> updateState() }
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        grant.setOnClickListener {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSION
            )
        }

        updateState()
    }

    private fun updateState() {
        if (!hasView) return
        val hasPermissions = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        msg.text = if (hasPermissions) {
            "Cool we got the permission."
        } else {
            "Click the button below to grant permissions."
        }
        grant.visibility = if (!hasPermissions) View.VISIBLE else View.GONE
    }

    private companion object {
        private const val REQUEST_CODE_PERMISSION = 1
    }
}