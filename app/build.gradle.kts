plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

android {
    namespace = "com.seniorhub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.seniorhub"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // For easier debugging; keep minify off
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // KAPT configuration
    kapt {
        // Disable build cache to avoid KAPT issues
        useBuildCache = false
        // Add JVM arguments for better error handling
        javacOptions {
            option("-Xmaxerrs", 500)
        }
        // Add error handling
        correctErrorTypes = true
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

dependencies {
    // AndroidX Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    implementation("com.google.firebase:firebase-analytics:21.5.0")

    // For coroutines support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // For JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // AndroidX Lifecycle (for lifecycleScope)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Google Play Services
    implementation(libs.play.services.auth)

    // Firebase (using BOM for version alignment)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.perf.ktx)
    implementation ("com.google.android.material:material:1.9.0")

    // Image Loading
    implementation(libs.glide)
    implementation(libs.google.material)
    kapt(libs.glide.compiler)

    // Cloudinary Android SDK
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    // For image compression before upload
    implementation("id.zelory:compressor:3.0.1")

    // Google Play Services Location (for FusedLocationProviderClient)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Apply parcelize plugin
apply(plugin = "org.jetbrains.kotlin.plugin.parcelize")
