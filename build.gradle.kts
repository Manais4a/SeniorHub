/**
 * Top-level build file for SeniorHub project
 * Configuration options common to all sub-projects/modules
 *
 * @author SeniorHub Team
 * @version 1.0.0
 */

plugins {
    // Android Application plugin - use latest stable version
    id("com.android.application") version "8.9.1" apply false

    // Kotlin Android plugin - use compatible version with Android Gradle Plugin
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false

    // Kotlin KAPT plugin for annotation processors
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false

    // Google Services plugin for Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false

    // Firebase Crashlytics plugin
    id("com.google.firebase.crashlytics") version "2.9.9" apply false

    // Firebase Performance plugin
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
}