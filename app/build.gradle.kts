// ------------------------------------------------------------
// app/build.gradle.kts
// Plugin versions are declared in the PROJECT-LEVEL build.gradle.kts.
// Do NOT put version numbers here — that causes duplicate-version conflicts.
// ------------------------------------------------------------
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.glitchlabyrinth"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.glitchlabyrinth"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

// kotlin { } must be at the TOP LEVEL of the file, NOT inside android { }.
// Placing it inside android { } binds it to ApplicationExtension instead of the project.
kotlin {
    jvmToolchain(11)
}

dependencies {
    // Compose BOM — pins all Compose library versions consistently
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))

    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3
    implementation("androidx.compose.material3:material3")

    // Activity Compose integration
    implementation("androidx.activity:activity-compose:1.9.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Material Components — provides Theme.Material3.DayNight.NoActionBar used in themes.xml.
    // The Compose material3 library alone does NOT supply XML theme resources;
    // this MDC-Android library does.
    implementation("com.google.android.material:material:1.12.0")

    // Core KTX
    implementation("androidx.core:core-ktx:1.13.1")

    // Debug only
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}