// Top-level build file: plugin versions come from gradle/libs.versions.toml
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// The project sources live inside OneDrive, whose sync engine locks and
// de-materializes files in build/ mid-build (observed: undeletable Hilt
// dirs, KSP outputs turned into cloud placeholders). Keep ALL build output
// on plain local disk instead — sources stay synced, outputs stay fast.
allprojects {
    layout.buildDirectory.set(
        File("C:/Users/rhanrichardson/android-dev/gradle-build/wah_gwaan/${project.name}"))
}
