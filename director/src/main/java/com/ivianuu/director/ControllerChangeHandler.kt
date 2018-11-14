package com.ivianuu.director

import android.os.Bundle
import android.view.View
import android.view.ViewGroup

import com.ivianuu.director.internal.newInstanceOrThrow

/**
 * ControllerChangeHandlers are responsible for swapping the View for one Controller to the View
 * of another. They can be useful for performing animations and transitions between Controllers. Several
 * default ControllerChangeHandlers are included.
 */
abstract class ControllerChangeHandler {

    /**
     * Whether or not this handler removes the from view on push
     */
    open val removesFromViewOnPush get() = forceRemoveViewOnPush

    open var forceRemoveViewOnPush = false

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
     * Will be called on change handlers that push a controller if the controller being pushed is
     * popped before it has completed.
     */
    open fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
    }

    /**
     * Will be called on change handlers that push a controller if the controller being pushed is
     * needs to be attached immediately, without any animations or transitions.
     */
    open fun completeImmediately() {
    }

    /**
     * Returns a copy of this ControllerChangeHandler. This method is internally used by the library, so
     * ensure it will return an exact copy of your handler if overriding. If not overriding, the handler
     * will be saved and restored from the Bundle format.
     */
    open fun copy() = fromBundle(toBundle())

    internal fun toBundle() = Bundle().apply {
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