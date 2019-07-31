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

package com.ivianuu.director.common.changehandler

import android.annotation.TargetApi
import android.app.SharedElementCallback
import android.graphics.Rect
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import com.ivianuu.director.ChangeData
import com.ivianuu.director.DirectorPlugins
import com.ivianuu.director.common.addTargets
import com.ivianuu.director.common.findNamedView
import com.ivianuu.director.common.findNamedViews
import com.ivianuu.director.common.getBoundsOnScreen
import com.ivianuu.director.common.replaceTargets
import com.ivianuu.director.common.setEpicenter
import com.ivianuu.director.common.setTargets
import com.ivianuu.director.common.transitionSetOf
import com.ivianuu.director.defaultRemovesFromViewOnPush

/**
 * [TransitionChangeHandler] which does shared element transitions
 */
@TargetApi(21)
abstract class SharedElementTransitionChangeHandler(
    duration: Long = DirectorPlugins.defaultTransitionDuration,
    removesFromViewOnPush: Boolean = DirectorPlugins.defaultRemovesFromViewOnPush
) : TransitionChangeHandler(duration, removesFromViewOnPush) {

    private val sharedElementNames = mutableMapOf<String, String>()
    private val waitForTransitionNames = mutableListOf<String>()
    private val removedViews = mutableListOf<Pair<View, ViewGroup>>()

    private var exitTransition: Transition? = null
    private var enterTransition: Transition? = null
    private var sharedElementTransition: Transition? = null
    private var exitTransitionCallback: SharedElementCallback? = null
    private var enterTransitionCallback: SharedElementCallback? = null

    override fun getTransition(changeData: ChangeData): Transition {
        exitTransition = getExitTransition(changeData)
        enterTransition = getEnterTransition(changeData)
        sharedElementTransition = getSharedElementTransition(changeData)
        exitTransitionCallback = getExitTransitionCallback(changeData)
        enterTransitionCallback = getEnterTransitionCallback(changeData)

        check(enterTransition != null || sharedElementTransition != null || exitTransition != null) {
            "SharedElementTransitionChangeHandler must have at least one transition."
        }

        return mergeTransitions(changeData.isPush)
    }

    override fun prepareForTransition(
        changeData: ChangeData,
        transition: Transition,
        onTransitionPrepared: () -> Unit
    ) {
        val listener = {
            configureTransition(changeData, transition)
            onTransitionPrepared()
        }

        configureSharedElements(changeData)

        val to = changeData.to

        if (to != null && to.parent == null && waitForTransitionNames.isNotEmpty()) {
            waitOnAllTransitionNames(to, listener)
            changeData.callback.addToView()
        } else {
            listener()
        }
    }

    override fun executePropertyChanges(
        changeData: ChangeData,
        transition: Transition?
    ) {
        val to = changeData.to
        if (to != null && removedViews.isNotEmpty()) {
            to.visibility = View.VISIBLE
            removedViews.forEach { it.second.addView(it.first) }
            removedViews.clear()
        }

        super.executePropertyChanges(changeData, transition)
    }

    private fun configureTransition(changeData: ChangeData, transition: Transition) {
        val (container) = changeData

        val nonExistentView = View(container.context)

        val fromSharedElements = mutableListOf<View>()
        val toSharedElements = mutableListOf<View>()

        configureSharedElements(
            changeData,
            nonExistentView,
            fromSharedElements,
            toSharedElements
        )

        val exitTransition = exitTransition
        val exitingViews = if (exitTransition != null) {
            configureEnteringExitingViews(
                exitTransition,
                changeData.from,
                fromSharedElements,
                nonExistentView
            )
        } else {
            emptyList()
        }

        if (exitingViews.isEmpty()) {
            this.exitTransition = null
        }

        enterTransition?.addTarget(nonExistentView)

        val enteringViews = mutableListOf<View>()

        scheduleRemoveTargets(
            transition,
            enterTransition,
            enteringViews,
            exitTransition,
            exitingViews,
            sharedElementTransition,
            toSharedElements
        )

        scheduleTargetChange(
            container,
            changeData.to,
            nonExistentView,
            toSharedElements,
            enteringViews,
            exitingViews.toMutableList()
        )

        container.setNameOverrides(toSharedElements)
        container.scheduleNameReset(toSharedElements)
    }

    private fun waitOnAllTransitionNames(
        to: View,
        onTransitionPrepared: () -> Unit
    ) {
        val onPreDrawListener = object : OnPreDrawListener {
            private var addedSubviewListeners = false
            override fun onPreDraw(): Boolean {
                val foundViews = mutableListOf<View>()
                var allViewsFound = true

                for (transitionName in waitForTransitionNames) {
                    val namedView = to.findNamedView(transitionName)
                    if (namedView != null) {
                        foundViews.add(namedView)
                    } else {
                        allViewsFound = false
                        break
                    }
                }

                if (allViewsFound && !addedSubviewListeners) {
                    addedSubviewListeners = true
                    waitOnChildTransitionNames(to, foundViews, this, onTransitionPrepared)
                }

                return false
            }
        }

        to.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
    }

    private fun waitOnChildTransitionNames(
        to: View,
        foundViews: List<View>,
        parentPreDrawListener: OnPreDrawListener,
        onTransitionPrepared: () -> Unit
    ) {
        foundViews.forEach {
            OneShotPreDrawListener(true, it) {
                waitForTransitionNames.remove(it.transitionName)
                removedViews.add(it to it.parent as ViewGroup)
                (it.parent as ViewGroup).removeView(it)

                if (waitForTransitionNames.isEmpty()) {
                    to.viewTreeObserver.removeOnPreDrawListener(parentPreDrawListener)
                    to.visibility = View.INVISIBLE
                    onTransitionPrepared()
                }
            }
        }
    }

    private fun scheduleTargetChange(
        container: ViewGroup,
        to: View?,
        nonExistentView: View,
        toSharedElements: List<View>,
        enteringViews: MutableList<View>,
        exitingViews: MutableList<View>
    ) {
        OneShotPreDrawListener(true, container) {
            val enterTransition = enterTransition
            if (enterTransition != null) {
                enterTransition.removeTarget(nonExistentView)
                val views = configureEnteringExitingViews(
                    enterTransition,
                    to,
                    toSharedElements,
                    nonExistentView
                )
                enteringViews.addAll(views)
            }

            val exitTransition = exitTransition
            if (exitTransition != null) {
                val tempExiting = mutableListOf<View>()
                tempExiting.add(nonExistentView)
                exitTransition.replaceTargets(exitingViews, tempExiting)
            }
            exitingViews.clear()
            exitingViews.add(nonExistentView)
        }
    }

    private fun mergeTransitions(isPush: Boolean): Transition {
        val overlap =
            enterTransition == null || exitTransition == null || allowTransitionOverlap(isPush)

        return if (overlap) {
            transitionSetOf(
                TransitionSet.ORDERING_TOGETHER,
                exitTransition, enterTransition, sharedElementTransition
            )
        } else {
            transitionSetOf(
                TransitionSet.ORDERING_TOGETHER,
                transitionSetOf(
                    TransitionSet.ORDERING_SEQUENTIAL,
                    exitTransition,
                    enterTransition
                ),
                sharedElementTransition
            )
        }
    }

    private fun configureEnteringExitingViews(
        transition: Transition,
        view: View?,
        sharedElements: List<View>,
        nonExistentView: View
    ): List<View> {
        val views = mutableListOf<View>()
        view?.captureTransitioningViews(views)
        views.removeAll(sharedElements)
        if (views.isNotEmpty()) {
            views.add(nonExistentView)
            transition.addTargets(views)
        }
        return views
    }

    private fun configureSharedElements(
        changeData: ChangeData,
        nonExistentView: View,
        fromSharedElements: MutableList<View>,
        toSharedElements: MutableList<View>
    ) {
        val (container, from, to, isPush) = changeData

        if (to == null || from == null) {
            return
        }

        val capturedFromSharedElements =
            captureFromSharedElements(from)

        if (sharedElementNames.isEmpty()) {
            sharedElementTransition = null
        } else {
            fromSharedElements.addAll(capturedFromSharedElements.values)
        }

        if (enterTransition == null && exitTransition == null && sharedElementTransition == null) {
            return
        }

        callSharedElementStartEnd(capturedFromSharedElements, true)

        val toEpicenter: Rect?
        val sharedElementTransition = sharedElementTransition
        if (sharedElementTransition != null) {
            toEpicenter = Rect()
            sharedElementTransition.setTargets(
                nonExistentView,
                fromSharedElements
            )
            setFromEpicenter(capturedFromSharedElements)
            val enterTransition = enterTransition
            if (enterTransition != null) {
                enterTransition.epicenterCallback = object : Transition.EpicenterCallback() {
                    override fun onGetEpicenter(transition: Transition): Rect? {
                        return if (toEpicenter.isEmpty) {
                            null
                        } else toEpicenter
                    }
                }
            }
        } else {
            toEpicenter = null
        }

        OneShotPreDrawListener(true, container) {
            val capturedToSharedElements = captureToSharedElements(to, isPush)

            toSharedElements.addAll(capturedToSharedElements.values)
            toSharedElements.add(nonExistentView)

            callSharedElementStartEnd(capturedToSharedElements, false)

            if (sharedElementTransition != null) {
                sharedElementTransition.targets.clear()
                sharedElementTransition.targets.addAll(toSharedElements)
                sharedElementTransition.replaceTargets(
                    fromSharedElements,
                    toSharedElements
                )

                val toEpicenterView = getToEpicenterView(capturedToSharedElements)
                if (toEpicenterView != null && toEpicenter != null) {
                    toEpicenterView.getBoundsOnScreen(toEpicenter)
                }
            }
        }
    }

    private fun getToEpicenterView(toSharedElements: MutableMap<String, View>) =
        if (enterTransition != null && sharedElementNames.isNotEmpty()) {
            toSharedElements[sharedElementNames.values.first()]
        } else null

    private fun setFromEpicenter(fromSharedElements: MutableMap<String, View>) {
        if (sharedElementNames.isNotEmpty()) {
            val fromEpicenterView = fromSharedElements[sharedElementNames.keys.first()]
            if (fromEpicenterView != null) {
                sharedElementTransition?.setEpicenter(fromEpicenterView)
                exitTransition?.setEpicenter(fromEpicenterView)
            }
        }
    }

    private fun captureToSharedElements(to: View?, isPush: Boolean): MutableMap<String, View> {
        if (sharedElementNames.isEmpty() || sharedElementTransition == null || to == null) {
            sharedElementNames.clear()
            return mutableMapOf()
        }

        val toSharedElements = mutableMapOf<String, View>()
        to.findNamedViews(toSharedElements)

        removedViews.forEach { toSharedElements[it.first.transitionName] = it.first }

        val names = sharedElementNames.values.toList()

        toSharedElements
            .filterKeys { !names.contains(it) }
            .forEach { toSharedElements.remove(it.key) }

        val enterTransitionCallback = enterTransitionCallback
        if (enterTransitionCallback != null) {
            enterTransitionCallback.onMapSharedElements(names, toSharedElements)

            names.reversed()
                .map { it to toSharedElements[it] }
                .forEach { (name, view) ->
                    if (view == null) {
                        val key = sharedElementNames.findKeyForValue(name)
                        if (key != null) {
                            sharedElementNames.remove(key)
                        }
                    } else if (name != view.transitionName) {
                        val key = sharedElementNames.findKeyForValue(name)
                        if (key != null) {
                            sharedElementNames[key] = view.transitionName
                        }
                    }
                }
        } else {
            sharedElementNames.toList()
                .reversed()
                .filterNot { toSharedElements.contains(it.second) }
                .forEach { sharedElementNames.remove(it.first) }
        }
        return toSharedElements
    }

    private fun <K, V> Map<K, V>.findKeyForValue(value: V): K? {
        return filterValues { it == value }
            .keys
            .firstOrNull()
    }

    private fun captureFromSharedElements(from: View): MutableMap<String, View> {
        if (sharedElementNames.isEmpty() || sharedElementTransition == null) {
            sharedElementNames.clear()
            return mutableMapOf()
        }

        val fromSharedElements = mutableMapOf<String, View>()
        from.findNamedViews(fromSharedElements)

        val names = sharedElementNames.keys.toList()

        fromSharedElements
            .filterKeys { !names.contains(it) }
            .forEach { fromSharedElements.remove(it.key) }

        val exitTransitionCallback = exitTransitionCallback
        if (exitTransitionCallback != null) {
            exitTransitionCallback.onMapSharedElements(names, fromSharedElements)

            names
                .reversed()
                .map { it to fromSharedElements[it] }
                .forEach { (name, view) ->
                    if (view == null) {
                        sharedElementNames.remove(name)
                    } else if (name != view.transitionName) {
                        val targetValue = sharedElementNames.remove(name)!!
                        sharedElementNames[view.transitionName] = targetValue
                    }
                }
        } else {
            sharedElementNames
                .filterKeys { !fromSharedElements.contains(it) }
                .forEach { sharedElementNames.remove(it.key) }
        }
        return fromSharedElements
    }

    private fun callSharedElementStartEnd(
        sharedElements: Map<String, View>,
        isStart: Boolean
    ) {
        val enterTransitionCallback = enterTransitionCallback
        if (enterTransitionCallback != null) {
            if (isStart) {
                enterTransitionCallback.onSharedElementStart(
                    sharedElements.keys.toList(),
                    sharedElements.values.toList(), null
                )
            } else {
                enterTransitionCallback.onSharedElementEnd(
                    sharedElements.keys.toList(),
                    sharedElements.values.toList(), null
                )
            }
        }
    }

    private fun View.captureTransitioningViews(transitioningViews: MutableList<View>) {
        if (visibility == View.VISIBLE) {
            if (this is ViewGroup) {
                if (isTransitionGroup) {
                    transitioningViews.add(this)
                } else {
                    (0 until childCount)
                        .map { getChildAt(it) }
                        .forEach { it.captureTransitioningViews(transitioningViews) }
                }
            } else {
                transitioningViews.add(this)
            }
        }
    }

    private fun scheduleRemoveTargets(
        overallTransition: Transition,
        enterTransition: Transition?,
        enteringViews: List<View>,
        exitTransition: Transition?,
        exitingViews: List<View>,
        sharedElementTransition: Transition?,
        toSharedElements: List<View>
    ) {
        overallTransition.addListener(object : TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                enterTransition?.replaceTargets(enteringViews, emptyList())
                exitTransition?.replaceTargets(exitingViews, emptyList())
                sharedElementTransition?.replaceTargets(toSharedElements, emptyList())
            }

            override fun onTransitionResume(transition: Transition) {
            }

            override fun onTransitionPause(transition: Transition) {
            }

            override fun onTransitionCancel(transition: Transition) {
            }

            override fun onTransitionEnd(transition: Transition) {
            }
        })
    }

    private fun ViewGroup.setNameOverrides(toSharedElements: List<View>) {
        OneShotPreDrawListener(true, this) {
            toSharedElements.forEach {
                val name = it.transitionName
                if (name != null) {
                    it.transitionName = sharedElementNames.findKeyForValue(name)
                }
            }
        }
    }

    private fun ViewGroup.scheduleNameReset(toSharedElements: List<View>) {
        OneShotPreDrawListener(true, this) {
            toSharedElements.forEach {
                val name = it.transitionName
                val inName = sharedElementNames[name]
                it.transitionName = inName
            }
        }
    }

    /**
     * Will be called when views are ready to have their shared elements configured. Within this method one of the addSharedElement methods
     * should be called for each shared element that will be used. If one or more of these shared elements will not instantly be available in
     * the incoming view (for ex, in a RecyclerView), waitOnSharedElementNamed can be called to delay the transition until everything is available.
     */
    protected abstract fun configureSharedElements(changeData: ChangeData)

    /**
     * Should return the transition that will be used on the exiting ("from") view, if one is desired.
     */
    protected open fun getExitTransition(changeData: ChangeData): Transition? = null

    /**
     * Should return the transition that will be used on shared elements between the from and to views.
     */
    protected open fun getSharedElementTransition(changeData: ChangeData): Transition? = null

    /**
     * Should return the transition that will be used on the entering ("to") view, if one is desired.
     */
    protected open fun getEnterTransition(changeData: ChangeData): Transition? = null

    /**
     * Should return a callback that can be used to customize transition behavior of the shared element transition for the "from" view.
     */
    protected open fun getExitTransitionCallback(changeData: ChangeData): SharedElementCallback? =
        null

    /**
     * Should return a callback that can be used to customize transition behavior of the shared element transition for the "to" view.
     */
    protected open fun getEnterTransitionCallback(changeData: ChangeData): SharedElementCallback? =
        null

    /**
     * Should return whether or not the the exit transition and enter transition should overlap. If true,
     * the enter transition will start as soon as possible. Otherwise, the enter transition will wait until the
     * completion of the exit transition. Defaults to true.
     */
    open fun allowTransitionOverlap(isPush: Boolean): Boolean = true

    /**
     * Used to register an element that will take part in the shared element transition.
     */
    protected fun addSharedElement(name: String) {
        sharedElementNames[name] = name
    }

    /**
     * Used to register an element that will take part in the shared element transition. Maps the name used in the
     * "from" view to the name used in the "to" view if they are not the same.
     */
    protected fun addSharedElement(fromName: String, toName: String) {
        sharedElementNames[fromName] = toName
    }

    /**
     * Used to register an element that will take part in the shared element transition. Maps the name used in the
     * "from" view to the name used in the "to" view if they are not the same.
     */
    protected fun addSharedElement(sharedElement: View, toName: String) {
        val transitionName = requireNotNull(sharedElement.transitionName) {
            "Unique transitionNames are required for all sharedElements"
        }
        sharedElementNames[transitionName] = toName
    }

    /**
     * Used to register an element that will take part in the shared element transition. Maps the name used in the
     * "from" view to the name used in the "to" view if they are not the same.
     */
    protected fun addSharedElement(from: View, to: View) {
        val fromName = requireNotNull(from.transitionName) {
            "Unique transitionNames are required for all sharedElements"
        }
        val toName = requireNotNull(to.transitionName) {
            "Unique transitionNames are required for all sharedElements"
        }
        sharedElementNames[fromName] = toName
    }

    /**
     * The transition will be delayed until the view with the name passed in is available in the "to" hierarchy. This is
     * particularly useful for views that don't load instantly, like RecyclerViews. Note that using this method can
     * potentially lock up your app indefinitely if the view never loads!
     */
    protected fun waitOnSharedElementNamed(name: String) {
        check(sharedElementNames.values.contains(name)) {
            "Can't wait on a shared element that hasn't been registered using addSharedElement"
        }
        waitForTransitionNames.add(name)
    }

    private class OneShotPreDrawListener(
        private val preDrawReturnValue: Boolean,
        private val view: View,
        private val action: () -> Unit
    ) : OnPreDrawListener, View.OnAttachStateChangeListener {

        private var viewTreeObserver = view.viewTreeObserver

        init {
            viewTreeObserver.addOnPreDrawListener(this)
            view.addOnAttachStateChangeListener(this)
        }

        override fun onPreDraw(): Boolean {
            removeListener()
            action()
            return preDrawReturnValue
        }

        private fun removeListener() {
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnPreDrawListener(this)
            } else {
                view.viewTreeObserver.removeOnPreDrawListener(this)
            }

            view.removeOnAttachStateChangeListener(this)
        }

        override fun onViewAttachedToWindow(v: View) {
            viewTreeObserver = v.viewTreeObserver
        }

        override fun onViewDetachedFromWindow(v: View) {
            removeListener()
        }

    }

}