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

package com.ivianuu.director.common

import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import com.ivianuu.stdlibx.firstNotNullResultOrNull

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object TransitionUtils {

    fun findNamedViews(namedViews: MutableMap<String, View>, view: View) {
        if (view.visibility != View.VISIBLE) return

        view.transitionName?.let { namedViews[it] = view }

        if (view is ViewGroup) {
            (0 until view.childCount)
                .map { view.getChildAt(it) }
                .forEach { findNamedViews(namedViews, it) }
        }
    }

    fun findNamedView(view: View, transitionName: String): View? {
        if (transitionName == view.transitionName) return view
        if (view !is ViewGroup) return null
        return (0 until view.childCount)
            .firstNotNullResultOrNull { findNamedView(view.getChildAt(it), transitionName) }
    }

    fun setEpicenter(transition: Transition, view: View) {
        val epicenter = Rect()
        getBoundsOnScreen(view, epicenter)
        transition.epicenterCallback = object : Transition.EpicenterCallback() {
            override fun onGetEpicenter(transition: Transition): Rect {
                return epicenter
            }
        }
    }

    fun getBoundsOnScreen(view: View, epicenter: Rect) {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        epicenter.set(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

    fun setTargets(transition: Transition, nonExistentView: View, sharedViews: MutableList<View>) {
        val views = transition.targets
        views.clear()
        sharedViews.forEach {
            bfsAddViewChildren(
                views,
                it
            )
        }
        views.add(nonExistentView)
        sharedViews.add(nonExistentView)
        addTargets(transition, sharedViews)
    }

    fun addTargets(transition: Transition, views: List<View>) {
        if (transition is TransitionSet) {
            (0 until transition.transitionCount)
                .map { transition.getTransitionAt(it) }
                .forEach { addTargets(it, views) }
        } else if (!hasSimpleTarget(transition)) {
            val targets = transition.targets
            if (isNullOrEmpty(targets)) {
                views.forEach { transition.addTarget(it) }
            }
        }
    }

    fun replaceTargets(transition: Transition, oldTargets: List<View>, newTargets: List<View>) {
        if (transition is TransitionSet) {
            (0 until transition.transitionCount)
                .map { transition.getTransitionAt(it) }
                .forEach {
                    replaceTargets(
                        it,
                        oldTargets,
                        newTargets
                    )
                }
        } else if (!hasSimpleTarget(transition)) {
            val targets = transition.targets
            if (targets != null && targets.size == oldTargets.size && targets.containsAll(oldTargets)) {
                newTargets.forEach { transition.addTarget(it) }
                oldTargets.forEach { transition.removeTarget(it) }
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

    private fun containedBeforeIndex(views: List<View>, view: View, maxIndex: Int) =
        (0 until maxIndex)
            .map { views[it] }
            .any { it == view }

    private fun hasSimpleTarget(transition: Transition): Boolean {
        return (!isNullOrEmpty(
            transition.targetIds
        )
                || !isNullOrEmpty(transition.targetNames)
                || !isNullOrEmpty(transition.targetTypes))
    }

    private fun isNullOrEmpty(list: List<*>?): Boolean = list == null || list.isEmpty()

    fun mergeTransitions(ordering: Int, vararg transitions: Transition?) =
        TransitionSet().apply {
            this.ordering = ordering
            transitions.filterNotNull().forEach { addTransition(it) }
        }
}