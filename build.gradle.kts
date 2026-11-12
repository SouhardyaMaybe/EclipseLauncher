// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version rootProject.extra["agpVersion"].toString() apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.devtools.ksp") version rootProject.extra["kspVersion"].toString() apply false
}