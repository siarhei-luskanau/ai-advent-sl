# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Project Name:** ai-advent-sl
**Repository:** git@github.com:siarhei-luskanau/ai-advent-sl.git
**License:** MIT License (2026)
**Project Type:** Kotlin Multiplatform (KMP) Chat Application with LLM Client
**Targets:** Android, iOS, JVM Desktop
**JDK:** openjdk-25
**IDE:** IntelliJ IDEA / Android Studio

## Technology Stack

- **Kotlin:** 2.1.0
- **Compose Multiplatform:** 1.7.1 (Jetpack Compose for all platforms)
- **Ktor Client:** 3.0.2 (HTTP client for API calls)
- **Kotlinx Serialization:** 1.7.3 (JSON serialization)
- **Kotlinx Coroutines:** 1.9.0 (async operations)
- **Room Database:** 2.7.0-alpha12 (local persistence with KMP support)
- **DataStore:** 1.1.1 (preferences storage)
- **Navigation Compose:** 2.8.0-alpha10
- **Koin:** 4.0.0 (dependency injection)
- **Kotlinx DateTime:** 0.6.1 (date/time operations)

## Project Structure

```
composeApp/
├── src/
│   ├── commonMain/          # Shared code for all platforms
│   │   ├── kotlin/
│   │   │   └── com.example.aiadvent/
│   │   │       ├── App.kt                    # Main Compose app
│   │   │       ├── di/                       # Dependency injection
│   │   │       ├── data/                     # Data layer
│   │   │       │   ├── local/                # Room database
│   │   │       │   ├── remote/               # Ktor API client
│   │   │       │   ├── repository/           # Repository pattern
│   │   │       │   └── model/                # Data models
│   │   │       ├── domain/                   # Business logic
│   │   │       │   └── usecase/              # Use cases
│   │   │       ├── ui/                       # Presentation layer
│   │   │       │   ├── chat/                 # Chat screen
│   │   │       │   ├── navigation/           # Navigation
│   │   │       │   └── theme/                # Material theme
│   │   │       └── util/                     # Utilities (expect declarations)
│   │   └── composeResources/                 # Resources
│   ├── androidMain/         # Android-specific code
│   │   ├── kotlin/
│   │   │   └── com.example.aiadvent/
│   │   │       ├── MainActivity.kt
│   │   │       ├── AiAdventApplication.kt
│   │   │       ├── di/                       # Platform DI module
│   │   │       └── util/                     # actual implementations
│   │   └── AndroidManifest.xml
│   ├── iosMain/             # iOS-specific code
│   │   └── kotlin/
│   │       └── com.example.aiadvent/
│   │           ├── MainViewController.kt
│   │           ├── di/                       # Platform DI module
│   │           └── util/                     # actual implementations
│   └── desktopMain/         # JVM Desktop-specific code
│       └── kotlin/
│           └── com.example.aiadvent/
│               ├── main.kt
│               ├── di/                       # Platform DI module
│               └── util/                     # actual implementations
└── build.gradle.kts
```

## Architecture

This project follows **Clean Architecture** with three main layers:

### 1. Presentation Layer (UI)
- **Compose Multiplatform UI**: Shared across all platforms
- **ViewModels**: State management using StateFlow
- **Navigation**: Navigation Compose for screen transitions
- **Koin**: Constructor injection for ViewModels

### 2. Domain Layer
- **Use Cases**: Business logic encapsulation
  - `SendMessageUseCase`: Validates and sends messages to LLM
  - `GetMessagesUseCase`: Retrieves message history
- **Domain Models**: Platform-agnostic data models

### 3. Data Layer
- **Repository Pattern**: Single source of truth
- **Local Data Source**: Room Database with Flow for reactive updates
- **Remote Data Source**: Ktor Client for LLM API calls
- **Offline-First**: Save locally first, then sync to server

### Expect/Actual Pattern

Platform-specific code uses Kotlin's expect/actual mechanism:

**commonMain (expect):**
```kotlin
// util/DatabaseBuilder.kt
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
```

**Platform-specific (actual):**
- `androidMain`: Uses `Context.filesDir` for database path
- `iosMain`: Uses `NSDocumentDirectory` for database path
- `desktopMain`: Uses `user.home` directory for database path

## Development Commands

### Build All Targets
```bash
./gradlew build
```

### Android

```bash
# Build Android APK
./gradlew :composeApp:assembleDebug

# Install on device/emulator
./gradlew :composeApp:installDebug

# Run from IDE
# Open in Android Studio and run MainActivity
```

### iOS

```bash
# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Run in Xcode
# 1. Generate Xcode project (if needed)
# 2. Open in Xcode and run on simulator/device
```

### Desktop (JVM)

```bash
# Run desktop application
./gradlew :composeApp:run

# Create distributable package
./gradlew :composeApp:createDistributable

# Package as DMG (macOS)
./gradlew :composeApp:packageDmg
```

### Clean Build

```bash
./gradlew clean
```

## Dependency Management

Dependencies are managed using Gradle Version Catalog (`gradle/libs.versions.toml`):

```kotlin
// Access in build.gradle.kts
implementation(libs.ktor.client.core)
implementation(libs.room.runtime)
```

## Room Database

- **Entities**: `MessageEntity` in `data/local/entity/`
- **DAOs**: `MessageDao` with suspend functions and Flow
- **Database**: `AppDatabase` singleton provided by platform modules
- **KSP**: Room compiler runs for each target (Android, iOS, Desktop)

### Important Notes
- All DAO methods must be suspend functions on non-Android platforms
- Use `BundledSQLiteDriver` for consistent SQLite across platforms
- Database path is platform-specific (handled by expect/actual)

## Ktor Client

HTTP client configured in `data/remote/ApiClient.kt`:
- **ContentNegotiation**: JSON serialization with kotlinx.serialization
- **Logging**: HTTP logging for debugging
- **Platform Engines**: Android, Darwin (iOS), CIO (Desktop)

## Koin Dependency Injection

### Module Structure
- `appModule` (commonMain): Common dependencies
- `platformModule()` (platform-specific): Platform-specific dependencies (e.g., database)

### Initialization
- **Android**: `AiAdventApplication.onCreate()`
- **iOS**: `MainViewController` initialization
- **Desktop**: `main()` function

## Testing

```bash
# Run common tests
./gradlew :composeApp:cleanTestDebugUnitTest :composeApp:testDebugUnitTest

# Run Android tests
./gradlew :composeApp:connectedAndroidTest
```

## Common Issues and Solutions

### Issue: Database not found
**Solution**: Ensure platform module is included in Koin initialization and provides `AppDatabase` singleton.

### Issue: Ktor client crashes on platform
**Solution**: Verify correct engine is included in platform-specific dependencies (android/darwin/cio).

### Issue: expect/actual mismatch
**Solution**: Ensure all `expect` declarations in commonMain have corresponding `actual` implementations in all platform source sets.

### Issue: Room compilation fails
**Solution**: Check KSP is configured for all targets in `dependencies` block of `build.gradle.kts`.

## IDE Configuration

- **IntelliJ IDEA/Android Studio**: Open root project
- **Xcode** (iOS): Generate and open Xcode project for iOS target
- Use IntelliJ IDEA 2024.1+ or Android Studio Koala+ for best KMP support

## Key Files

- `composeApp/build.gradle.kts`: Module configuration for all targets
- `gradle/libs.versions.toml`: Centralized dependency versions
- `composeApp/src/commonMain/kotlin/com/example/aiadvent/di/AppModule.kt`: DI configuration
- `composeApp/src/*/kotlin/com/example/aiadvent/util/DatabaseBuilder.*.kt`: Platform-specific database setup
