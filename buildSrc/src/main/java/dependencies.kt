@file:Suppress("ClassName", "unused")

object Versions {
    // android
    const val compileSdk = 28
    const val minSdk = 16
    const val minSdkSample = 21
    const val targetSdk = 28
    const val versionCode = 1
    const val versionName = "0.0.1"
    const val groupId = "com.github.IVIanuu.director"

    const val androidGradlePlugin = "3.2.0"

    const val androidx = "1.0.0"
    const val androidxArch = "2.0.0-rc01"

    const val kotlin = "1.3.0-rc-57"

    const val leakCanary = "1.6.1"

    const val mavenGradlePlugin = "2.1"
    const val materialComponents = "1.0.0-rc01"
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
}