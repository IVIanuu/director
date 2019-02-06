package com.ivianuu.director.internal

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