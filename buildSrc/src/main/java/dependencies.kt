@file:Suppress("ClassName", "unused")

object Build {
    const val applicationId = "com.ivianuu.director.sample"
    const val buildToolsVersion = "28.0.3"
    const val compileSdk = 28
    const val minSdk = 16
    const val targetSdk = 28

    const val versionCode = 1
    const val versionName = "0.0.1"
}

object Versions {
    const val androidGradlePlugin = "3.2.1"

    const val androidx = "1.0.0"
    const val androidxArch = "2.0.0-rc01"

    const val kotlin = "1.3.0"

    const val leakCanary = "1.6.1"

    const val mavenGradlePlugin = "2.1"
    const val materialComponents = "1.0.0"

    const val rxJava = "2.2.2"

    const val scopes = "1a360d16ad"

    const val traveler = "21c2bf5f07"
}

object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"

    const val androidxAppCompat = "androidx.appcompat:appcompat:${Versions.androidx}"
    const val androidxFragment = "androidx.fragment:fragment:${Versions.androidx}"
    const val androidxViewPager = "androidx.viewpager:viewpager:${Versions.androidx}"

    const val archLifecycleExtensions =
        "androidx.lifecycle:lifecycle-extensions:${Versions.androidxArch}"

    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}"

    const val mavenGradlePlugin = "com.github.dcendents:android-maven-gradle-plugin:${Versions.mavenGradlePlugin}"

    const val materialComponents =
        "com.google.android.material:material:${Versions.materialComponents}"

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