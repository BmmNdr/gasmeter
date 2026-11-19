# GasMeter

An Android app that tracks how much fuel your daily trips use, based on the car's
"range remaining" reading. See [docs/SPEC.md](docs/SPEC.md) for the product spec.

## Project layout

- `core/`: pure Kotlin (JVM) module with the trip calculations, verdict rules and
  route statistics. No Android dependencies.
- `app/`: the Android app (Kotlin, Jetpack Compose, Room). Trip logging: start a trip,
  end it, and see the result card with the verdict for its route.
  - `data/`: Room database (schema exported to `app/schemas/`) and `TripRepository`.
  - `ui/`: screens (home, start trip, end trip, trip result) and their view models.

## Building

Requires JDK 17 or newer (JDK 21 for the app tests, which run Robolectric on Android SDK 36).

Run the calculation tests (no Android SDK needed):

```bash
./gradlew :core:test
```

Run the app tests: database, view models and full trip flows through the real UI,
on the JVM via Robolectric:

```bash
./gradlew :app:testDebugUnitTest
```

Optionally render screenshots of the main screens to a folder:

```bash
./gradlew :app:testDebugUnitTest --tests com.gasmeter.app.ScreenshotTest -PscreenshotDir=/tmp/gasmeter-screens
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
