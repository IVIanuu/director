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

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-build-app.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-android-ext.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-kapt.gradle")

android {
    defaultConfig {
        minSdkVersion(Build.minSdkSample)
    }

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(Deps.androidxAppCompat)

    implementation(Deps.dagger)
    kapt(Deps.daggerCompiler)

    implementation(project(":director"))
    implementation(project(":director-activitycallbacks"))
    implementation(project(":director-androidx-lifecycle"))
    implementation(project(":director-common"))
    implementation(project(":director-dialog"))
    implementation(project(":director-fragmenthost"))
    implementation(project(":director-scopes"))
    implementation(project(":director-traveler"))
    implementation(project(":director-viewpager"))

    implementation(Deps.epoxy)
    implementation(Deps.epoxyKtx)
    kapt(Deps.epoxyProcessor)

    implementation(Deps.leakCanary)
    implementation(Deps.materialComponents)

    implementation(Deps.rxJava)

    implementation(Deps.scopesRx)

    implementation("com.afollestad.material-dialogs:core:0.9.6.0")

    testImplementation(project(":director-testing"))
    testImplementation(Deps.androidxTestCore)
    testImplementation(Deps.androidxTestJunit)
    testImplementation(Deps.androidxTestRules)
    testImplementation(Deps.androidxTestRunner)
    testImplementation(Deps.junit)
    testImplementation(Deps.mockitoKotlin)
    testImplementation(Deps.roboelectric)
}