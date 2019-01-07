package com.ivianuu.director.sample

import dagger.Component
import javax.inject.Inject
import javax.inject.Scope

@Scope
annotation class GrandParentScope

@GrandParentScope
@Component
interface GrandParentComponent {
    fun grantParentDep()
}

@GrandParentScope
class GrandParentDep @Inject constructor()

@Scope
annotation class ParentScope

/**
 * @author Manuel Wrage (IVIanuu)
 */
@ParentScope
@Component
interface ParentComponent {
    fun parentDep(): ParentDep
}

@ParentScope
class ParentDep @Inject constructor()

@Scope
annotation class ChildScope

@ChildScope
@Component(dependencies = [GrandParentComponent::class, ParentComponent::class])
interface ChildComponent {
    fun childDep(): ChildDep
    fun grandParentDep(): GrandParentDep
    fun parentDep(): ParentDep
}

@ChildScope
class ChildDep @Inject constructor(
    private val grandParentDep: GrandParentDep,
    private val parentDep: ParentDep
)