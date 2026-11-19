pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "gasmeter"

// Pure-Kotlin calculation logic. Builds and tests without the Android SDK.
include(":core")

// The Android app needs the Android SDK. Skip it when no SDK is configured so
// `./gradlew :core:test` still works on a plain JVM machine.
val localProperties = file("local.properties")
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    (localProperties.exists() && localProperties.readText().contains("sdk.dir"))

if (hasAndroidSdk) {
    include(":app")
} else {
    logger.warn("Android SDK not found (ANDROID_HOME or local.properties sdk.dir); skipping :app")
}
