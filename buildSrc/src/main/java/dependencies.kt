@file:Suppress("ClassName", "unused")

object Build {
    const val applicationId = "com.ivianuu.director.sample"
    const val buildToolsVersion = "28.0.3"
    const val compileSdk = 28
    const val minSdk = 16
    const val minSdkSample = 21
    const val targetSdk = 28

    const val versionCode = 1
    const val versionName = "0.0.1"
}

object Versions {
    const val androidGradlePlugin = "3.2.1"

    const val androidx = "1.0.0"
    const val androidxLifecycle = "2.0.0"
    const val androidxTestRules = "1.1.0"
    const val androidxTestRunner = "1.1.0"

    const val dagger = "2.19"

    const val epoxy = "2.19.0"
    const val epoxyKtx = "1a273d9917"

    const val junit = "4.12"

    const val kotlin = "1.3.10"

    const val leakCanary = "1.6.1"

    const val mavenGradlePlugin = "2.1"
    const val materialComponents = "1.0.0"

    const val mockitoKotlin = "2.0.0"

    const val roboelectric = "4.0.2"

    const val rxJava = "2.2.2"

    const val scopes = "6c630efcd6"

    const val traveler = "e1cb60c864"
}

object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"

    const val androidxAppCompat = "androidx.appcompat:appcompat:${Versions.androidx}"
    const val androidxFragment = "androidx.fragment:fragment:${Versions.androidx}"
    const val androidxLifecycleExtensions =
        "androidx.lifecycle:lifecycle-extensions:${Versions.androidxLifecycle}"
    const val androidxViewPager = "androidx.viewpager:viewpager:${Versions.androidx}"

    const val androidxTestCore = "androidx.test:core:${Versions.androidx}"
    const val androidxTestJunit = "androidx.test.ext:junit:${Versions.androidx}"
    const val androidxTestRules = "androidx.test:rules:${Versions.androidxTestRules}"
    const val androidxTestRunner = "androidx.test:runner:${Versions.androidxTestRunner}"

    const val dagger = "com.google.dagger:dagger:${Versions.dagger}"
    const val daggerCompiler = "com.google.dagger:dagger-compiler:${Versions.dagger}"

    const val epoxy = "com.airbnb.android:epoxy:${Versions.epoxy}"
    const val epoxyProcessor = "com.airbnb.android:epoxy-processor:${Versions.epoxy}"
    const val epoxyKtx = "com.github.IVIanuu:epoxy-ktx:${Versions.epoxyKtx}"

    const val junit = "junit:junit:${Versions.junit}"

    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}"

    const val mavenGradlePlugin = "com.github.dcendents:android-maven-gradle-plugin:${Versions.mavenGradlePlugin}"

    const val materialComponents =
        "com.google.android.material:material:${Versions.materialComponents}"

    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}"

    const val roboelectric = "org.robolectric:robolectric:${Versions.roboelectric}"

    const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"

    const val scopes = "com.github.IVIanuu.scopes:scopes:${Versions.scopes}"
    const val scopesCache = "com.github.IVIanuu.scopes:scopes-cache:${Versions.scopes}"
    const val scopesLifecycle = "com.github.IVIanuu.scopes:scopes-lifecycle:${Versions.scopes}"
    const val scopesRx = "com.github.IVIanuu.scopes:scopes-rx:${Versions.scopes}"

    const val traveler = "com.github.IVIanuu.traveler:traveler:${Versions.traveler}"
    const val travelerAndroid = "com.github.IVIanuu.traveler:traveler-android:${Versions.traveler}"
    const val travelerCommon =
        "com.github.IVIanuu.traveler:traveler-common:${Versions.traveler}"
}