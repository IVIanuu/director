package com.ivianuu.director

import android.os.Bundle
import com.ivianuu.director.internal.newInstanceOrThrow

/**
 * Swaps views on controller changes
 */
abstract class ControllerChangeHandler {

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

    internal open fun copy(): ControllerChangeHandler = fromBundle(toBundle())

    interface Callback {
        fun addToView()
        fun toViewAdded()
        fun removeFromView()
        fun fromViewRemoved()
        fun onChangeCompleted()
    }

    internal fun toBundle(): Bundle = Bundle().apply {
        classLoader = this@ControllerChangeHandler::class.java.classLoader

        putString(KEY_CLASS_NAME, this@ControllerChangeHandler.javaClass.name)
        putBundle(KEY_SAVED_STATE, Bundle()
            .also { it.classLoader = this@ControllerChangeHandler::class.java.classLoader }
            .also { this@ControllerChangeHandler.saveInstanceState(it) })
    }

    companion object {
        private const val KEY_CLASS_NAME = "ControllerChangeHandler.className"
        private const val KEY_SAVED_STATE = "ControllerChangeHandler.savedState"

        internal fun fromBundle(bundle: Bundle): ControllerChangeHandler {
            val className = bundle.getString(KEY_CLASS_NAME)!!
            return newInstanceOrThrow<ControllerChangeHandler>(className).apply {
                restoreInstanceState(bundle.getBundle(KEY_SAVED_STATE)!!)
            }
        }
    }
}