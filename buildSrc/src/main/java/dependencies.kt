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

object Publishing {
    const val groupId = "com.ivianuu.director"
    const val vcsUrl = "https://github.com/IVIanuu/director"
    const val version = "${Build.versionName}-dev-18"
}

object Versions {
    const val androidGradlePlugin = "3.3.0"

    const val androidxAppCompat = "1.1.0-alpha01"
    const val androidxFragment = "1.1.0-alpha03"
    const val androidxLifecycle = "2.0.0"
    const val androidxTestCore = "1.0.0"
    const val androidxTestJunit = "1.0.0"
    const val androidxTestRules = "1.1.0"
    const val androidxTestRunner = "1.1.0"
    const val androidxViewPager = "1.0.0"

    const val bintray = "1.8.4"

    const val dagger = "2.19"

    const val epoxy = "3.0.0"
    const val epoxyKtx = "0.0.1-dev-1"

    const val junit = "4.12"

    const val kotlin = "1.3.21"

    const val leakCanary = "1.6.1"

    const val mavenGradlePlugin = "2.1"
    const val materialComponents = "1.0.0"

    const val mockitoKotlin = "2.0.0"

    const val roboelectric = "4.0.2"

    const val rxJava = "2.2.6"

    const val scopes = "0.0.1-dev-3"

    const val stdlibx = "0.0.1-dev-3"

    const val traveler = "0.0.1-dev-1"
}

object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"

    const val androidxAppCompat = "androidx.appcompat:appcompat:${Versions.androidxAppCompat}"
    const val androidxFragment = "androidx.fragment:fragment:${Versions.androidxFragment}"
    const val androidxLifecycleExtensions =
        "androidx.lifecycle:lifecycle-extensions:${Versions.androidxLifecycle}"
    const val androidxViewPager = "androidx.viewpager:viewpager:${Versions.androidxViewPager}"

    const val androidxTestCore = "androidx.test:core:${Versions.androidxTestCore}"
    const val androidxTestJunit = "androidx.test.ext:junit:${Versions.androidxTestJunit}"
    const val androidxTestRules = "androidx.test:rules:${Versions.androidxTestRules}"
    const val androidxTestRunner = "androidx.test:runner:${Versions.androidxTestRunner}"

    const val bintrayGradlePlugin =
        "com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintray}"

    const val dagger = "com.google.dagger:dagger:${Versions.dagger}"
    const val daggerCompiler = "com.google.dagger:dagger-compiler:${Versions.dagger}"

    const val epoxy = "com.airbnb.android:epoxy:${Versions.epoxy}"
    const val epoxyProcessor = "com.airbnb.android:epoxy-processor:${Versions.epoxy}"
    const val epoxyKtx = "com.ivianuu.epoxyktx:epoxyktx:${Versions.epoxyKtx}"

    const val junit = "junit:junit:${Versions.junit}"

    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"

    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}"

    const val mavenGradlePlugin = "com.github.dcendents:android-maven-gradle-plugin:${Versions.mavenGradlePlugin}"

    const val materialComponents =
        "com.google.android.material:material:${Versions.materialComponents}"

    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}"

    const val roboelectric = "org.robolectric:robolectric:${Versions.roboelectric}"

    const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"

    const val scopes = "com.ivianuu.scopes:scopes:${Versions.scopes}"
    const val scopesCache = "com.ivianuu.scopes:scopes-cache:${Versions.scopes}"
    const val scopesLifecycle = "com.ivianuu.scopes:scopes-lifecycle:${Versions.scopes}"
    const val scopesRx = "com.ivianuu.scopes:scopes-rx:${Versions.scopes}"

    const val stdlibx = "com.ivianuu.stdlibx:stdlibx:${Versions.stdlibx}"

    const val traveler = "com.ivianuu.traveler:traveler:${Versions.traveler}"
    const val travelerAndroid = "com.ivianuu.traveler:traveler-android:${Versions.traveler}"
    const val travelerCommon =
        "com.ivianuu.traveler:traveler-common:${Versions.traveler}"
}