# EclipseLauncher

A modern Android game launcher built with Kotlin, Jetpack Compose, and Material 3.

## Tech Stack

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose (Material 3)
- **Design System**: Compose Material 3 (M3) `1.5.0-beta01`
- **Adaptive Layout**: Material 3 Adaptive `1.0.0` + Navigation Suite `1.5.0-rc01`
- **Async**: Kotlin Coroutines & Flow `1.8.1`
- **Image Loading**: Coil `2.6.0`
- **Build**: Gradle 8.5 (KTS), AGP 8.5.0
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Project Structure

```
EclipseLauncher/
├── app/
│   ├── src/main/
│   │   ├── java/com/eclipselauncher/
│   │   │   ├── EclipseLauncherApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── ui/theme/
│   │   │       ├── Theme.kt
│   │   │       └── Typography.kt
│   │   ├── res/
│   │   │   ├── values/strings.xml, colors.xml, themes.xml
│   │   │   └── values-night/colors.xml, themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/wrapper/
├── .github/workflows/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── README.md
```

## Getting Started

### Prerequisites
- JDK 17+
- Android SDK 34
- Gradle 8.5 (via wrapper)

### Build Commands (Run in CI/CD)
```bash
# Make wrapper executable
chmod +x gradlew

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Lint
./gradlew lint
```

## CI/CD

All builds run in GitHub Actions (`.github/workflows/`). No local building required.

## Features (Planned)
- Game detection and management
- Java/LWJGL game launching via JNI
- GLFW/SDL3 integration for windowing
- Adaptive UI for phone/tablet/desktop
- Material 3 theming with dynamic color

## License
TBD# CI trigger
