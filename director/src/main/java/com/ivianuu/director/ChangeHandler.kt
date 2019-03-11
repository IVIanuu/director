package com.ivianuu.director

import android.os.Bundle
import com.ivianuu.director.internal.newInstanceOrThrow

/**
 * Swaps views on controller changes
 */
abstract class ChangeHandler {

    /**
     * Whether or not this changeHandler removes the from view on push
     */
    open val removesFromViewOnPush: Boolean get() = true

    internal var hasBeenUsed = false

    /**
     * Responsible for swapping Views from one Controller to another.
     */
    abstract fun performChange(changeData: ChangeData)

    /**
     * Saves any data about this changeHandler to a Bundle in case the application is killed.
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
     * Returns a exact copy of this change handler
     */
    open fun copy(): ChangeHandler = fromBundle(toBundle())

    internal fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_CLASS_NAME, this@ChangeHandler.javaClass.name)
        putBundle(KEY_SAVED_STATE, Bundle().also(this@ChangeHandler::saveToBundle))
    }

    companion object {
        private const val KEY_CLASS_NAME = "ChangeHandler.className"
        private const val KEY_SAVED_STATE = "ChangeHandler.savedState"

        internal fun fromBundle(bundle: Bundle): ChangeHandler {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            return newInstanceOrThrow<ChangeHandler>(className).apply {
                restoreFromBundle(bundle.getBundle(KEY_SAVED_STATE)!!)
            }
        }
    }
}