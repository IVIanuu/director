package com.ivianuu.director.internal

import android.view.View
import android.view.ViewGroup
import java.util.*

fun <T : Any> newInstanceOrThrow(className: String): T = try {
    classForNameOrThrow<T>(className).newInstance() as T
} catch (e: Exception) {
    throw RuntimeException("could not instantiate $className, $e")
}

fun <T> classForNameOrThrow(className: String): Class<out T> = try {
    Class.forName(className) as Class<out T>
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
    val allViews = (0 until childCount).map { getChildAt(it) }.toMutableList()
    Collections.swap(allViews, index, to)
    allViews.forEach { it.bringToFront() }
    requestLayout()
    invalidate()
}