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

package com.ivianuu.director.util

import android.content.Context
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.view.View
import android.widget.FrameLayout
import java.io.FileDescriptor

class AttachFakingFrameLayout(context: Context) : FrameLayout(context) {

    private val fakeWindowToken: IBinder = object : IBinder {
        override fun getInterfaceDescriptor(): String? = null

        override fun pingBinder(): Boolean = false

        override fun isBinderAlive(): Boolean = false

        override fun queryLocalInterface(descriptor: String): IInterface? = null

        override fun dump(fd: FileDescriptor, args: Array<String>) {
        }

        override fun dumpAsync(fd: FileDescriptor, args: Array<String>) {
        }

        override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = false

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
        }

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int): Boolean = false
    }

    private var reportAttached = false

    override fun getWindowToken(): IBinder? = if (reportAttached) fakeWindowToken else null

    @JvmOverloads fun setAttached(attached: Boolean, reportToViewUtils: Boolean = true) {
        if (reportAttached != attached) {
            reportAttached = attached
            if (reportToViewUtils) {
                ViewUtils.reportAttached(this, attached)
            }

            for (i in 0 until childCount) {
                ViewUtils.reportAttached(getChildAt(i), attached)
            }
        }
    }

    override fun onViewAdded(child: View) {
        if (reportAttached) {
            ViewUtils.reportAttached(child, true)
        }
        super.onViewAdded(child)
    }

    override fun onViewRemoved(child: View) {
        ViewUtils.reportAttached(child, false)
        super.onViewRemoved(child)
    }
}