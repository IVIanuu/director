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

package com.ivianuu.director.activitycallbacks

import com.ivianuu.director.Controller
import com.ivianuu.director.doOnPostDestroy

/**
 * Listener for multi window change events
 */
interface MultiWindowModeChangeListener {
    /**
     * Will be called when ever the multi window mode changes
     */
    fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean)
}

/**
 * Whether or not the hosting activity is currently in multi window mode
 */
val Controller.isInMultiWindowMode: Boolean
    get() = activityCallbacks.isInMultiWindowMode

/**
 * Notifies the [listener] on multi window mode changes
 */
fun Controller.addMultiWindowModeChangeListener(
    listener: MultiWindowModeChangeListener
) {
    // remove the listener on controller destruction
    doOnPostDestroy {
        activityCallbacks
            .removeMultiWindowModeChangeListener(listener)
    }

    activityCallbacks.addMultiWindowModeChangeListener(listener)
}

/**
 * Invokes [onMultiWindowModeChanged] on multi window mode changes
 */
fun Controller.addMultiWindowModeChangeListener(
    onMultiWindowModeChanged: (isInMultiWindowMode: Boolean) -> Unit
): MultiWindowModeChangeListener {
    val listener = object : MultiWindowModeChangeListener {
        override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
            onMultiWindowModeChanged.invoke(isInMultiWindowMode)
        }
    }

    addMultiWindowModeChangeListener(listener)

    return listener
}

/**
 * Listener for picture in picture mode events
 */
interface PictureInPictureModeChangeListener {
    /**
     * Will be called when ever picture in picture mode changes
     */
    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean)
}

/**
 * Whether or not the hosting activity is currently in picture in picture mode
 */
val Controller.isInPictureInPictureMode: Boolean
    get() = activityCallbacks.isInPictureInPictureMode

/**
 * Notifies the [listener] on multi window mode changes
 */
fun Controller.addPictureInPictureModeChangeListener(
    listener: PictureInPictureModeChangeListener
) {
    // remove the listener on controller destruction
    doOnPostDestroy {
        activityCallbacks
            .removePictureInPictureModeChangeListener(listener)
    }

    activityCallbacks.addPictureInPictureModeChangeListener(listener)
}

/**
 * Invokes [onPictureInPictureModeChanged] on picture in picture mode changes
 */
fun Controller.addPictureInPictureModeChangeListener(
    onPictureInPictureModeChanged: (isInMultiWindowMode: Boolean) -> Unit
): PictureInPictureModeChangeListener {
    val listener = object : PictureInPictureModeChangeListener {
        override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
            onPictureInPictureModeChanged.invoke(isInPictureInPictureMode)
        }
    }

    addPictureInPictureModeChangeListener(listener)

    return listener
}