// Top-level build file where you can add configuration options common to all sub-projects/modules.

// /build.gradle.kts
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.android.library") version "8.6.1" apply false
    // Kotlin は Compose Compiler 1.5.15 と互換の 1.9.25 を維持
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
}


