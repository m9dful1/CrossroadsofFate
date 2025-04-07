// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // These come from your version catalogs. Make sure they're referencing
    // a recent version of the Android Gradle plugin and Kotlin plugin.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false

}

// If you're not using the 'buildscript { ... }' block for older Gradle versions,
// you can omit it. Your version catalogs handle plugin versions.