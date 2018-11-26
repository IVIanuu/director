package com.ivianuu.director.internal

import android.os.Bundle
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerFactory
import com.ivianuu.director.RouterTransaction
import java.util.*

internal class Backstack {

    val entries: List<RouterTransaction> get() = _entries.toList()
    private val _entries = mutableListOf<RouterTransaction>()

    val reversedEntries get() = _entries.reversed()

    val size get() = _entries.size

    fun pop() = _entries.removeAt(_entries.lastIndex)

    fun peek() = _entries.lastOrNull()

    fun push(transaction: RouterTransaction) {
        _entries.add(transaction)
    }

    fun popAll(): List<RouterTransaction> {
        val entries = reversedEntries
        _entries.clear()
        return entries
    }

    fun setEntries(entries: List<RouterTransaction>) {
        _entries.clear()
        _entries.addAll(entries)
    }

    fun remove(transaction: RouterTransaction) {
        _entries.remove(transaction)
    }

    fun contains(controller: Controller) = _entries.any { it.controller == controller }

    fun saveInstanceState(outState: Bundle) {
        val entryBundles = _entries.map { it.saveInstanceState() }
        outState.putParcelableArrayList(KEY_ENTRIES, ArrayList(entryBundles))
    }

    fun restoreInstanceState(savedInstanceState: Bundle, controllerFactory: ControllerFactory) {
        savedInstanceState.getParcelableArrayList<Bundle>(KEY_ENTRIES)
            ?.forEach { _entries.add(RouterTransaction.fromBundle(it, controllerFactory)) }
    }

    companion object {
        private const val KEY_ENTRIES = "Backstack.entries"
    }
}
