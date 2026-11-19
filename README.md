# GasMeter

An Android app that tracks how much fuel your daily trips use, based on the car's
"range remaining" reading. See [docs/SPEC.md](docs/SPEC.md) for the product spec.

## Project layout

- `core/`: pure Kotlin (JVM) module with the trip calculations, verdict rules and
  route statistics. No Android dependencies.
- `app/`: the Android app (Kotlin, Jetpack Compose). Currently a placeholder screen.

## Building

Requires JDK 17 or newer.

Run the calculation tests (no Android SDK needed):

```bash
./gradlew :core:test
```

Build the debug APK (needs the Android SDK with platform 36; set `ANDROID_HOME` or
put `sdk.dir=/path/to/sdk` in `local.properties`, or open the project in Android Studio):

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Without an
Android SDK the `app` module is skipped automatically.

## Toolchain

Android Gradle Plugin 8.13, Kotlin 2.2, Gradle 8.14, compileSdk/targetSdk 36, minSdk 26.
