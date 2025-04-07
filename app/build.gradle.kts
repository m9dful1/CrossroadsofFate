plugins {
    // References to your version catalog plugin aliases:
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // Needed for Room annotation processing
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    // Add the Compose Compiler plugin
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.spiritwisestudios.crossroadsoffate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spiritwisestudios.crossroadsoffate"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            // If you use Vector Drawables, enable support library
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // Disable minification for now (you can adjust as needed)
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Ensure your sourceCompatibility/targetCompatibility match recommended Java versions.
    // Compose 1.5.1 works with Java 1.8 or higher. Java 17 is recommended.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Enable Compose
    buildFeatures {
        compose = true
    }

    // Exclude certain resources from packaging if needed
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    // Robolectric configuration
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}
kapt {
    correctErrorTypes = true
}
// Declare your dependencies, referencing version-catalog entries where possible
dependencies {
    // Core AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Compose Activity integration
    implementation(libs.androidx.activity.compose)

    // Compose BOM + UI libraries
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)                // e.g. androidx.compose.ui:ui
    implementation(libs.androidx.ui.graphics)       // e.g. androidx.compose.ui:ui-graphics
    implementation(libs.androidx.ui.tooling.preview) // e.g. androidx.compose.ui:ui-tooling-preview
    implementation(libs.androidx.material3)         // e.g. androidx.compose.material3:material3

    // Room for database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.junit.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // JSON parsing
    implementation(libs.gson)

    // Timber for logging
    implementation(libs.timber)

    // For the 'viewModel()' function in Compose:
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Testing dependencies
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    
    // Room testing
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.truth)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    
    // Android instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.benchmark.junit4)
    
    // Debug dependencies for tests
    debugImplementation(libs.ui.test.manifest)
    }