package com.ivianuu.director.internal

import com.ivianuu.director.RouterTransaction

fun <T : Any> newInstanceOrThrow(className: String) = try {
    classForNameOrThrow<T>(className).newInstance() as T
} catch (e: Exception) {
    throw RuntimeException("could not instantiate $className, $e")
}

fun <T> classForNameOrThrow(className: String) = try {
    Class.forName(className) as Class<out T>
} catch (e: Exception) {
    throw RuntimeException("couldn't find class $className")
}

internal fun List<RouterTransaction>.filterVisible(): List<RouterTransaction> {
    val visible = mutableListOf<RouterTransaction>()

    for (transaction in reversed()) {
        visible.add(transaction)
        if (transaction.pushChangeHandler == null
            || transaction.pushChangeHandler!!.removesFromViewOnPush
        ) {
            break
        }
    }

    return visible.reversed()
}

internal fun backstacksAreEqual(
    lhs: List<RouterTransaction>,
    rhs: List<RouterTransaction>
): Boolean {
    if (lhs.size != rhs.size) return false

    lhs.forEachIndexed { i, transaction ->
        if (transaction != rhs[i]) return false
    }

    return true
}