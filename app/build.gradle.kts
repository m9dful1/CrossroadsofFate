plugins {
    // References to your version catalog plugin aliases:
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // Needed for Room annotation processing
    id("org.jetbrains.kotlin.kapt")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Enable Compose
    buildFeatures {
        compose = true
    }
    composeOptions {
        // IMPORTANT: Must match the Compose version used in your version catalog.
        // e.g., if you're using Compose BOM 2023.08.00 or 1.5.1, set this accordingly.
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    // Exclude certain resources from packaging if needed
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // For the 'viewModel()' function in Compose:
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}