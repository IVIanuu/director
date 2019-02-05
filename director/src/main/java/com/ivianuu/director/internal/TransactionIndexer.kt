package com.ivianuu.director.internal

import android.os.Bundle

internal class TransactionIndexer {

    private var currentIndex = 0

    fun nextIndex(): Int = ++currentIndex

    fun saveInstanceState(): Bundle = Bundle().apply {
        putInt(KEY_INDEX, currentIndex)
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {
        currentIndex = savedInstanceState.getInt(KEY_INDEX)
    }

    companion object {
        private const val KEY_INDEX = "TransactionIndexer.currentIndex"
    }
}