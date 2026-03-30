// FILE: build.gradle.kts (Root Project)
plugins {
    // Use 'alias' to load versions from libs.versions.toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}