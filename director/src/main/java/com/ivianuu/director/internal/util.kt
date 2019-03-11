package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup
import java.util.*

fun <T : Any> newInstanceOrThrow(className: String): T = try {
    classForNameOrThrow(className).newInstance() as T
} catch (e: Exception) {
    throw RuntimeException("could not instantiate $className, $e")
}

fun classForNameOrThrow(className: String): Class<*> = try {
    Class.forName(className)
} catch (e: Exception) {
    throw RuntimeException("couldn't find class $className")
}

fun ViewGroup.moveView(view: View, to: Int) {
    if (to == -1) {
        view.bringToFront()
        return
    }
    val index = indexOfChild(view)
    if (index == -1 || index == to) return
    val allViews = (0 until childCount).map(this::getChildAt).toMutableList()
    Collections.swap(allViews, index, to)
    allViews.forEach(View::bringToFront)
    requestLayout()
    invalidate()
}