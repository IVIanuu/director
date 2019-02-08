/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:TargetApi(Build.VERSION_CODES.LOLLIPOP)

package com.ivianuu.director.common

import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import com.ivianuu.stdlibx.firstNotNullResultOrNull

fun View.findNamedViews(namedViews: MutableMap<String, View>) {
    if (visibility != View.VISIBLE) return

    transitionName?.let { namedViews[it] = this }

    if (this is ViewGroup) {
        (0 until childCount)
            .map { getChildAt(it) }
            .forEach { it.findNamedViews(namedViews) }
    }
}

fun View.findNamedView(transitionName: String): View? {
    if (transitionName == this.transitionName) return this
    if (this !is ViewGroup) return null
    return (0 until childCount)
        .firstNotNullResultOrNull { getChildAt(it).findNamedView(transitionName) }
}

fun Transition.setEpicenter(view: View) {
    val viewEpicenter = Rect().also { view.getBoundsOnScreen(it) }
    epicenterCallback = object : Transition.EpicenterCallback() {
        override fun onGetEpicenter(transition: Transition): Rect = viewEpicenter
    }
}

fun View.getBoundsOnScreen(rect: Rect) {
    val loc = IntArray(2)
    getLocationOnScreen(loc)
    rect.set(loc[0], loc[1], loc[0] + width, loc[1] + height)
}

fun Transition.setTargets(nonExistentView: View, sharedViews: MutableList<View>) {
    val views = targets
    views.clear()
    sharedViews.forEach {
        bfsAddViewChildren(
            views,
            it
        )
    }
    views.add(nonExistentView)
    sharedViews.add(nonExistentView)
    addTargets(sharedViews)
}

fun Transition.addTargets(views: List<View>) {
    if (this is TransitionSet) {
        (0 until transitionCount)
            .map { getTransitionAt(it) }
            .forEach { it.addTargets(views) }
    } else if (!hasSimpleTarget && targets.isNullOrEmpty()) {
        views.forEach { addTarget(it) }
    }
}

fun Transition.replaceTargets(oldTargets: List<View>, newTargets: List<View>) {
    if (this is TransitionSet) {
        (0 until transitionCount)
            .map { getTransitionAt(it) }
            .forEach {
                it.replaceTargets(
                    oldTargets,
                    newTargets
                )
            }
    } else if (!hasSimpleTarget) {
        if (targets != null && targets.size == oldTargets.size && targets.containsAll(oldTargets)) {
            newTargets.forEach { addTarget(it) }
            oldTargets.forEach { removeTarget(it) }
        }
    }
}

private fun bfsAddViewChildren(views: MutableList<View>, startView: View) {
    val startIndex = views.size
    if (containedBeforeIndex(
            views,
            startView,
            startIndex
        )
    ) {
        return  // This child is already in the list, so all its children are also.
    }
    views.add(startView)

    // drop?
    (startIndex until views.size)
        .map { views[it] }
        .filterIsInstance<ViewGroup>()
        .flatMap { viewGroup -> (0 until viewGroup.childCount).map { viewGroup.getChildAt(it) } }
        .filterNot {
            containedBeforeIndex(
                views,
                it,
                startIndex
            )
        }
        .forEach { views.add(it) }
}

private fun containedBeforeIndex(views: List<View>, view: View, maxIndex: Int): Boolean =
    (0 until maxIndex)
        .map { views[it] }
        .any { it == view }

private val Transition.hasSimpleTarget: Boolean
    get() = (!targetIds.isNullOrEmpty()
            || !targetNames.isNullOrEmpty()
            || !targetTypes.isNullOrEmpty())

fun transitionSetOf(ordering: Int, vararg transitions: Transition?): TransitionSet =
    TransitionSet().apply {
        this.ordering = ordering
        transitions.filterNotNull().forEach { addTransition(it) }
    }