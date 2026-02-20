// ------------------------------------------------------------
// PROJECT-LEVEL build.gradle.kts  (root of the project, NOT inside app/)
// This is the ONLY place plugin versions are declared.
// Each module applies them without a version number.
// ------------------------------------------------------------
plugins {
    id("com.android.application")          version "9.0.1" apply false
    id("com.android.library")             version "9.0.1" apply false
    id("org.jetbrains.kotlin.android")    version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}