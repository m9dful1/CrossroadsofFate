plugins {
    // References to your version catalog plugin aliases:
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // Needed for Room annotation processing
    id("com.google.devtools.ksp")
    // Compose Compiler plugin (version follows the Kotlin version in the catalog)
    alias(libs.plugins.composeCompiler)
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

    // Enable Compose (compiler version is managed by the Compose Compiler plugin)
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
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // JSON parsing
    implementation(libs.gson)

    // Timber for logging
    implementation(libs.timber)

    // For the 'viewModel()' function in Compose:
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Unit tests (JVM / Robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Android instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug-only tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}