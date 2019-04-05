package com.ivianuu.director

import android.os.Bundle

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
     * Will be called when the change must be canceled
     */
    open fun cancel() {
    }

    /**
     * Saves any data about this changeHandler to a Bundle in case the application is killed.
     */
    open fun saveInstanceState(outState: Bundle) {
    }

    /**
     * Restores data that was saved in [saveInstanceState].
     */
    open fun restoreInstanceState(savedInstanceState: Bundle) {
    }

    internal open fun copy(): ChangeHandler = fromBundle(toBundle())

    interface Callback {
        fun addToView()
        fun removeFromView()
        fun onChangeCompleted()
    }

    internal fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_CLASS_NAME, this@ChangeHandler.javaClass.name)
        putBundle(KEY_SAVED_STATE, Bundle().also(this@ChangeHandler::saveInstanceState))
    }

    companion object {
        private const val KEY_CLASS_NAME = "ChangeHandler.className"
        private const val KEY_SAVED_STATE = "ChangeHandler.savedState"

        internal fun fromBundle(bundle: Bundle): ChangeHandler {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            return newInstanceOrThrow<ChangeHandler>(className).apply {
                restoreInstanceState(bundle.getBundle(KEY_SAVED_STATE)!!)
            }
        }
    }
}