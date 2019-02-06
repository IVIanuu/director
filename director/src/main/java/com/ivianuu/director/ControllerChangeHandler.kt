package com.ivianuu.director

import android.os.Bundle
import android.view.View
import android.view.ViewGroup

import com.ivianuu.director.internal.newInstanceOrThrow

/**
 * Swaps views on controller changes
 */
abstract class ControllerChangeHandler {

    /**
     * Whether or not this handler removes the from view on push
     */
    open val removesFromViewOnPush: Boolean get() = true

    internal var forceRemoveViewOnPush = false
    internal var hasBeenUsed = false

    /**
     * Responsible for swapping Views from one Controller to another.
     */
    abstract fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        onChangeComplete: () -> Unit
    )

    /**
     * Saves any data about this handler to a Bundle in case the application is killed.
     */
    open fun saveToBundle(bundle: Bundle) {
    }

    /**
     * Restores data that was saved in the [saveToBundle] method.
     */
    open fun restoreFromBundle(bundle: Bundle) {
    }

    /**
     * Will be called when the change must be canceled
     */
    open fun cancel(immediate: Boolean) {
    }

    /**
     * Returns a copy of this ControllerChangeHandler. This method is internally used by the library, so
     * ensure it will return an exact copy of your handler if overriding. If not overriding, the handler
     * will be saved and restored from the Bundle format.
     */
    open fun copy(): ControllerChangeHandler = fromBundle(toBundle())

    internal fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_CLASS_NAME, this@ControllerChangeHandler.javaClass.name)
        putBundle(KEY_SAVED_STATE, Bundle().also { saveToBundle(it) })
    }

    companion object {
        private const val KEY_CLASS_NAME = "ControllerChangeHandler.className"
        private const val KEY_SAVED_STATE = "ControllerChangeHandler.savedState"

        internal fun fromBundle(bundle: Bundle): ControllerChangeHandler {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            return newInstanceOrThrow<ControllerChangeHandler>(className).apply {
                restoreFromBundle(bundle.getBundle(KEY_SAVED_STATE)!!)
            }
        }
    }
}