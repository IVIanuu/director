import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension

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
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("com.github.dcendents.android-maven")
}

group = "com.github.ivianuu"

android {
    compileSdkVersion(Build.compileSdk)

    defaultConfig {
        buildToolsVersion = Build.buildToolsVersion
        minSdkVersion(Build.minSdk)
        targetSdkVersion(Build.targetSdk)
    }

    androidExtensions {
        // isExperimental = true
        configure(delegateClosureOf<AndroidExtensionsExtension> {
            isExperimental = true
        })
    }
}

dependencies {
    api(Deps.androidxFragment)
    api(Deps.kotlinStdLib)
}

if (project.hasProperty("android")) {
    task("sourcesJar", Jar::class) {
        from(android.sourceSets["main"].java.srcDirs)
        classifier = "sources"
    }

    task("javadoc", Javadoc::class) {
        isFailOnError = false
        source = android.sourceSets["main"].java.sourceFiles
        classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))
        classpath += configurations.compile
    }

    task("javadocJar", Jar::class) {
        val javadoc = dependsOn("javadoc")
        classifier = "javadoc"
        from(javadoc.property("destinationDir"))
    }
} else {
    task("sourcesJar", Jar::class) {
        dependsOn("classes")
        from(sourceSets.getByName("main").allSource)
        classifier = "sources"
    }

    task("javadocJar", Jar::class) {
        val javadoc = dependsOn("javadoc")
        classifier = "javadoc"
        from(javadoc.property("destinationDir"))
    }
}

artifacts {
    add("archives", tasks.getByName("sourcesJar"))
    add("archives", tasks.getByName("javadocJar"))
}