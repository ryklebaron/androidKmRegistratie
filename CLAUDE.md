# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app for kilometer registration for a small-business owner. Integrates with Android Auto (Car App Library) to log drives as business or private, reads the Volvo Connected Vehicle API for odometer values, and exports logs to CSV. See `instructions.md` for the original specification.

## Build Commands

The Gradle wrapper needs to be generated before first build. Either open the project in Android Studio (it will create `gradlew` + `gradle/wrapper/gradle-wrapper.jar` automatically), or run:

```bash
gradle wrapper --gradle-version 8.7   # requires Gradle installed globally
```

Once the wrapper exists:

```bash
./gradlew assembleDebug          # build debug APK
./gradlew assembleRelease        # build release APK
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device/emulator)
./gradlew test --tests "com.rykle.kmregistratie.SomeName"  # single test
./gradlew lint
./gradlew installDebug           # install on connected device
```

## Package Structure

```
com.rykle.kmregistratie
├── domain/          # Pure Kotlin — no Android/framework imports
│   ├── model/       # Drive, DriveType, LocationPoint
│   ├── repository/  # DriveRepository, VolvoRepository (interfaces)
│   ├── provider/    # LocationProvider (interface)
│   └── usecase/     # StartDrive, EndDrive, ContinueDrive, GetDrives, ExportDrives
├── data/
│   ├── local/
│   │   ├── dao/       # Room DAO
│   │   ├── database/  # KmDatabase
│   │   ├── entity/    # DriveEntity
│   │   └── preferences/ # AppPreferences (EncryptedSharedPreferences — VIN, token, API key)
│   ├── remote/        # VolvoApiService (Retrofit), OdometerResponse DTO
│   ├── repository/    # DriveRepositoryImpl, VolvoRepositoryImpl
│   └── provider/      # FusedLocationProviderImpl
├── di/              # Hilt modules: DatabaseModule, NetworkModule, RepositoryModule
├── service/         # Android Auto (Car App Library)
│   ├── KmCarAppService   # CarAppService entry point (AndroidEntryPoint)
│   ├── DriveSession      # Session — registers onDestroy → EndDriveUseCase
│   ├── ConnectionTracker # Tracks disconnect time in SharedPreferences (60s window)
│   ├── DriveTypeScreen   # AA screen: Business / Private / Continue list
│   └── DrivingScreen     # AA screen: "Rit actief" — keeps session alive while driving
└── ui/
    ├── MainActivity    # Compose + bottom nav (Ritten / Instellingen)
    ├── viewmodel/      # DriveViewModel (shared across screens)
    ├── screen/         # DriveListScreen, SettingsScreen
    └── theme/          # Material3 theme + MismatchRed color
```

## Key Architecture Decisions

**Android Auto session lifecycle**: `DriveSession.onDestroy` only fires on physical phone→car disconnect, not on in-app navigation. This is achieved by pushing `DrivingScreen` (instead of popping) after drive type selection, keeping the screen stack non-empty while driving.

**60-second continue window**: `ConnectionTracker` writes the disconnect timestamp to `SharedPreferences` on `onDestroy`. On reconnect, `canContinueDrive()` checks elapsed time. Selecting "continue" calls `ContinueDriveUseCase` (reopens the last completed drive in Room) then `connectionTracker.clearProvisionalEnd()`.

**Km mismatch detection**: When `StartDriveUseCase` runs, `startKm` (from Volvo API) is compared to the previous drive's `endKm`. If they differ, `Drive.hasKmMismatch = true`, which turns the card red in the list and adds a "KM MISMATCH" column in the CSV export.

**Volvo API authentication**: The access token is stored in `EncryptedSharedPreferences` via `AppPreferences`. The user pastes it manually in the Settings screen. `VolvoRepositoryImpl` sends it as `Authorization: Bearer <token>` with a `VCC-Api-Key` header. The Volvo Connected Vehicle API endpoint is `GET /connected-vehicle/v2/vehicles/{vin}/odometer`.

**Location fallback**: If `FusedLocationProviderClient` fails or permission is not granted, coordinates are stored as `0.0, 0.0`. The `@SuppressLint("MissingPermission")` annotation is present; runtime permission requests are not yet implemented in the UI.

## Tech Stack

- Kotlin 2.0.21, AGP 8.5.2, minSdk 26, targetSdk 35
- Jetpack Compose with Material3
- Car App Library 1.7.0 (phone projection, not AAOS)
- Room 2.6.1, Hilt 2.52, Retrofit 2.11.0 + Gson
- Play Services Location 21.3.0 (FusedLocationProviderClient)
- Security-Crypto 1.0.0 (EncryptedSharedPreferences for token storage)
