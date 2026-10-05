# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app for kilometer registration for a small-business owner. Drives are started and ended manually in the app (business or private); the Volvo Connected Vehicle API supplies odometer values, and logs export to CSV. There is deliberately no Android Auto integration and no background processing: the app does nothing unless the user opens it. `instructions.md` is the original specification; its Android Auto parts have been dropped.

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
│   └── usecase/     # StartDrive, EndDrive, GetDrives, ExportDrives, GetPendingKmDrive, ResolvePendingKm
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
└── ui/
    ├── MainActivity    # Compose + bottom nav (Ritten / Instellingen), hosts PendingKmDialog
    ├── viewmodel/      # DriveViewModel (shared across screens)
    ├── screen/         # DriveListScreen, DriveDetailScreen, SettingsScreen, PendingKmDialog
    └── theme/          # Material3 theme + MismatchRed color
```

## Key Architecture Decisions

**Pending end km**: the car only uploads its odometer to the Volvo cloud after it has been locked for a while, so at drive end the API often still returns the start value. `EndDriveUseCase` then sets `Drive.kmPending = true` (end km null or equal to start km). `DriveViewModel` loads the oldest pending drive on creation (= app start) and right after ending a drive; `PendingKmDialog` offers: confirm/edit end km (prefilled via `GetPendingKmDriveUseCase` with the next drive's start km, or else the current Volvo odometer if it differs from start km), re-sync, delete, or postpone until the next app start. `ResolvePendingKmUseCase` clears the flag and recomputes the next drive's mismatch. Pending drives count as unknown km in totals and get "EINDSTAND ONBEKEND" in the CSV.

**Km mismatch detection**: When `StartDriveUseCase` runs, `startKm` (from Volvo API) is compared to the previous drive's `endKm` (skipped if that drive is still `kmPending`). If they differ, `Drive.hasKmMismatch = true`, which turns the card red in the list and adds a "KM MISMATCH" column in the CSV export.

**Database migrations**: `KmDatabase` is at version 3 (`MIGRATION_2_3` adds `kmPending`). `fallbackToDestructiveMigration()` is still set, so every schema bump needs an explicit `Migration` or the user's drives get wiped.

**Volvo API authentication**: The access token is stored in `EncryptedSharedPreferences` via `AppPreferences`. The user pastes it manually in the Settings screen. `VolvoRepositoryImpl` sends it as `Authorization: Bearer <token>` with a `VCC-Api-Key` header. The Volvo Connected Vehicle API endpoint is `GET /connected-vehicle/v2/vehicles/{vin}/odometer`.

**Location fallback**: If `FusedLocationProviderClient` fails or permission is not granted, coordinates are stored as `0.0, 0.0`. The `@SuppressLint("MissingPermission")` annotation is present; runtime permission requests are not yet implemented in the UI.

## Tech Stack

- Kotlin 2.0.21, AGP 8.5.2, minSdk 26, targetSdk 35
- Jetpack Compose with Material3
- Room 2.6.1, Hilt 2.52, Retrofit 2.11.0 + Gson
- Play Services Location 21.3.0 (FusedLocationProviderClient)
- Security-Crypto 1.0.0 (EncryptedSharedPreferences for token storage)
