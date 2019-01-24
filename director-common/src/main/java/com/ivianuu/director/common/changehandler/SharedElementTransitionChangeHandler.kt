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
import android.os.Build
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import com.ivianuu.director.Controller
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.common.TransitionUtils

/**
 * A TransitionChangeHandler that facilitates using different Transitions for the entering view, the exiting view,
 * and shared elements between the two.
 */
// Much of this class is based on FragmentTransition.java and FragmentTransitionCompat21.java from the Android support library
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
abstract class SharedElementTransitionChangeHandler : TransitionChangeHandler() {

    // A map of from -> to names. Generally these will be the same.
    private val sharedElementNames = mutableMapOf<String, String>()
    private val waitForTransitionNames = mutableListOf<String>()
    private val removedViews = mutableListOf<ViewParentPair>()

    private var exitTransition: Transition? = null
    private var enterTransition: Transition? = null
    private var sharedElementTransition: Transition? = null
    private var exitTransitionCallback: SharedElementCallback? = null
    private var enterTransitionCallback: SharedElementCallback? = null

    override fun getTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition {
        exitTransition = getExitTransition(container, from, to, isPush)
        enterTransition = getEnterTransition(container, from, to, isPush)
        sharedElementTransition = getSharedElementTransition(container, from, to, isPush)
        exitTransitionCallback = getExitTransitionCallback(container, from, to, isPush)
        enterTransitionCallback = getEnterTransitionCallback(container, from, to, isPush)

        check(enterTransition != null || sharedElementTransition != null || exitTransition != null) {
            "SharedElementTransitionChangeHandler must have at least one transaction."
        }

        return mergeTransitions(isPush)
    }

    override fun prepareForTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition,
        isPush: Boolean,
        onTransitionPrepared: () -> Unit
    ) {
        val listener = {
            configureTransition(container, from, to, transition, isPush)
            onTransitionPrepared()
        }

        configureSharedElements(container, from, to, isPush)

        if (to != null && to.parent == null && waitForTransitionNames.isNotEmpty()) {
            waitOnAllTransitionNames(to, listener)
            container.addView(to)
        } else {
            listener()
        }
    }

    override fun executePropertyChanges(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition?,
        isPush: Boolean
    ) {
        if (to != null && removedViews.isNotEmpty()) {
            to.visibility = View.VISIBLE
            removedViews.forEach { it.parent.addView(it.view) }
            removedViews.clear()
        }

        super.executePropertyChanges(container, from, to, transition, isPush)
    }

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)
        removedViews.clear()
    }

    private fun configureTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition,
        isPush: Boolean
    ) {
        val nonExistentView = View(container.context)

        val fromSharedElements = mutableListOf<View>()
        val toSharedElements = mutableListOf<View>()

        configureSharedElements(
            container,
            nonExistentView,
            to,
            from,
            isPush,
            fromSharedElements,
            toSharedElements
        )

        val exitTransition = exitTransition
        val exitingViews = if (exitTransition != null) {
            configureEnteringExitingViews(
                exitTransition,
                from,
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
            to,
            nonExistentView,
            toSharedElements,
            enteringViews,
            exitingViews.toMutableList()
        )

        setNameOverrides(container, toSharedElements)
        scheduleNameReset(container, toSharedElements)
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
                    val namedView = TransitionUtils.findNamedView(
                        to,
                        transitionName
                    )
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
            OneShotPreDrawListener.add(
                true,
                it
            ) {
                waitForTransitionNames.remove(it.transitionName)

                removedViews.add(
                    ViewParentPair(
                        it,
                        it.parent as ViewGroup
                    )
                )
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
        OneShotPreDrawListener.add(
            true,
            container
        ) {
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
                TransitionUtils.replaceTargets(
                    exitTransition,
                    exitingViews,
                    tempExiting
                )
            }
            exitingViews.clear()
            exitingViews.add(nonExistentView)
        }
    }

    private fun mergeTransitions(isPush: Boolean): Transition {
        val overlap =
            enterTransition == null || exitTransition == null || allowTransitionOverlap(isPush)

        if (overlap) {
            return TransitionUtils.mergeTransitions(
                TransitionSet.ORDERING_TOGETHER,
                exitTransition,
                enterTransition,
                sharedElementTransition
            )
        } else {
            val staggered = TransitionUtils.mergeTransitions(
                TransitionSet.ORDERING_SEQUENTIAL,
                exitTransition,
                enterTransition
            )
            return TransitionUtils.mergeTransitions(
                TransitionSet.ORDERING_TOGETHER,
                staggered,
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
        val viewList = mutableListOf<View>()
        if (view != null) {
            captureTransitioningViews(viewList, view)
        }
        viewList.removeAll(sharedElements)
        if (!viewList.isEmpty()) {
            viewList.add(nonExistentView)
            TransitionUtils.addTargets(transition, viewList)
        }
        return viewList
    }

    private fun configureSharedElements(
        container: ViewGroup,
        nonExistentView: View,
        to: View?,
        from: View?,
        isPush: Boolean,
        fromSharedElements: MutableList<View>,
        toSharedElements: MutableList<View>
    ) {
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
            TransitionUtils.setTargets(
                sharedElementTransition,
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

        OneShotPreDrawListener.add(
            true,
            container
        ) {
            val capturedToSharedElements = captureToSharedElements(to, isPush)

            toSharedElements.addAll(capturedToSharedElements.values)
            toSharedElements.add(nonExistentView)

            callSharedElementStartEnd(capturedToSharedElements, false)

            if (sharedElementTransition != null) {
                sharedElementTransition.targets.clear()
                sharedElementTransition.targets.addAll(toSharedElements)
                TransitionUtils.replaceTargets(
                    sharedElementTransition,
                    fromSharedElements,
                    toSharedElements
                )

                val toEpicenterView = getToEpicenterView(capturedToSharedElements)
                if (toEpicenterView != null && toEpicenter != null) {
                    TransitionUtils.getBoundsOnScreen(
                        toEpicenterView,
                        toEpicenter
                    )
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
                sharedElementTransition?.let {
                    TransitionUtils.setEpicenter(
                        it,
                        fromEpicenterView
                    )
                }
                exitTransition?.let {
                    TransitionUtils.setEpicenter(
                        it,
                        fromEpicenterView
                    )
                }
            }
        }
    }

    private fun captureToSharedElements(to: View?, isPush: Boolean): MutableMap<String, View> {
        if (sharedElementNames.isEmpty() || sharedElementTransition == null || to == null) {
            sharedElementNames.clear()
            return mutableMapOf()
        }

        val toSharedElements = mutableMapOf<String, View>()
        TransitionUtils.findNamedViews(toSharedElements, to)

        removedViews.forEach { toSharedElements[it.view.transitionName] = it.view }

        val names = sharedElementNames.values.toList()

        toSharedElements
            .filterKeys { !names.contains(it) }
            .forEach { toSharedElements.remove(it.key) }

        val enterTransitionCallback = enterTransitionCallback
        if (enterTransitionCallback != null) {
            enterTransitionCallback.onMapSharedElements(names, toSharedElements)

            for (i in names.indices.reversed()) {
                val name = names[i]
                val view = toSharedElements[name]
                if (view == null) {
                    val key = findKeyForValue(sharedElementNames, name)
                    if (key != null) {
                        sharedElementNames.remove(key)
                    }
                } else if (name != view.transitionName) {
                    val key = findKeyForValue(sharedElementNames, name)
                    if (key != null) {
                        sharedElementNames[key] = view.transitionName
                    }
                }
            }
        } else {
            for (i in sharedElementNames.size - 1 downTo 0) {
                val targetName = sharedElementNames.values.elementAt(i)
                if (!toSharedElements.containsKey(targetName)) {
                    sharedElementNames.remove(sharedElementNames.keys.elementAt(i))
                }
            }
        }
        return toSharedElements
    }

    private fun findKeyForValue(map: MutableMap<String, String>, value: String): String? {
        return map
            .filterValues { it == value }
            .keys
            .firstOrNull()
    }

    private fun captureFromSharedElements(from: View): MutableMap<String, View> {
        if (sharedElementNames.isEmpty() || sharedElementTransition == null) {
            sharedElementNames.clear()
            return mutableMapOf()
        }

        val fromSharedElements = mutableMapOf<String, View>()
        TransitionUtils.findNamedViews(fromSharedElements, from)

        val names = sharedElementNames.keys.toList()

        fromSharedElements
            .filterKeys { !names.contains(it) }
            .forEach { fromSharedElements.remove(it.key) }

        val exitTransitionCallback = exitTransitionCallback
        if (exitTransitionCallback != null) {
            exitTransitionCallback.onMapSharedElements(names, fromSharedElements)
            for (i in names.indices.reversed()) {
                val name = names[i]
                val view = fromSharedElements[name]
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

    private fun captureTransitioningViews(transitioningViews: MutableList<View>, view: View) {
        if (view.visibility == View.VISIBLE) {
            if (view is ViewGroup) {
                if (view.isTransitionGroup) {
                    transitioningViews.add(view)
                } else {
                    (0 until view.childCount)
                        .map { view.getChildAt(it) }
                        .forEach { captureTransitioningViews(transitioningViews, it) }
                }
            } else {
                transitioningViews.add(view)
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
                if (enterTransition != null) {
                    TransitionUtils.replaceTargets(
                        enterTransition,
                        enteringViews,
                        emptyList()
                    )
                }
                if (exitTransition != null) {
                    TransitionUtils.replaceTargets(
                        exitTransition,
                        exitingViews,
                        emptyList()
                    )
                }
                if (sharedElementTransition != null) {
                    TransitionUtils.replaceTargets(
                        sharedElementTransition,
                        toSharedElements,
                        emptyList()
                    )
                }
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

    private fun setNameOverrides(container: View, toSharedElements: List<View>) {
        OneShotPreDrawListener.add(
            true,
            container
        ) {
            toSharedElements.forEach {
                val name = it.transitionName
                if (name != null) {
                    val inName = findKeyForValue(sharedElementNames, name)
                    it.transitionName = inName
                }
            }
        }
    }

    private fun scheduleNameReset(container: ViewGroup, toSharedElements: List<View>) {
        OneShotPreDrawListener.add(
            true,
            container
        ) {
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
    abstract fun configureSharedElements(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    )

    /**
     * Should return the transition that will be used on the exiting ("from") view, if one is desired.
     */
    abstract fun getExitTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition?

    /**
     * Should return the transition that will be used on shared elements between the from and to views.
     */
    abstract fun getSharedElementTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition?

    /**
     * Should return the transition that will be used on the entering ("to") view, if one is desired.
     */
    abstract fun getEnterTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition?

    /**
     * Should return a callback that can be used to customize transition behavior of the shared element transition for the "from" view.
     */
    open fun getExitTransitionCallback(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): SharedElementCallback? {
        return null
    }

    /**
     * Should return a callback that can be used to customize transition behavior of the shared element transition for the "to" view.
     */
    open fun getEnterTransitionCallback(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): SharedElementCallback? {
        return null
    }

    /**
     * Should return whether or not the the exit transition and enter transition should overlap. If true,
     * the enter transition will start as soon as possible. Otherwise, the enter transition will wait until the
     * completion of the exit transition. Defaults to true.
     */
    open fun allowTransitionOverlap(isPush: Boolean): Boolean {
        return true
    }

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
     * The transition will be delayed until the view with the name passed in is available in the "to" hierarchy. This is
     * particularly useful for views that don't load instantly, like RecyclerViews. Note that using this method can
     * potentially lock up your app indefinitely if the view never loads!
     */
    protected fun waitOnSharedElementNamed(name: String) {
        check(!sharedElementNames.values.contains(name)) {
            "Can't wait on a shared element that hasn't been registered using addSharedElement"
        }
        waitForTransitionNames.add(name)
    }

    private class OneShotPreDrawListener(
        private val preDrawReturnValue: Boolean,
        private val view: View,
        private val runnable: () -> Unit
    ) : OnPreDrawListener, View.OnAttachStateChangeListener {

        private var viewTreeObserver = view.viewTreeObserver

        override fun onPreDraw(): Boolean {
            removeListener()
            runnable()
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

        companion object {
            fun add(
                preDrawReturnValue: Boolean,
                view: View,
                runnable: () -> Unit
            ): OneShotPreDrawListener {
                val listener =
                    OneShotPreDrawListener(
                        preDrawReturnValue,
                        view,
                        runnable
                    )
                view.viewTreeObserver.addOnPreDrawListener(listener)
                view.addOnAttachStateChangeListener(listener)
                return listener
            }
        }
    }

    private data class ViewParentPair(
        val view: View,
        val parent: ViewGroup
    )
}