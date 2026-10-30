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

Gradle 8.x does not run on the system JDK 25; use `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.

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
│   └── usecase/     # StartDrive, EndDrive, GetDrives, ExportDrives/ImportDrives (DriveCsv), NextcloudSync, GetPendingKmDrive, ResolvePendingKm
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
    ├── MainActivity    # Compose + bottom nav (Ritten / Overzicht / Instellingen), hosts PendingKmDialog
    ├── viewmodel/      # DriveViewModel (shared across screens)
    ├── screen/         # DriveListScreen (search), DriveDetailScreen, OverviewScreen, SettingsScreen, PendingKmDialog, ProjectChooser
    └── theme/          # Always-dark theme in the baronpower.nl palette; Motion.kt holds the site's animations (reveal, shine, aurora, grid, flow dots)
```

## Key Architecture Decisions

**Pending end km**: the car only uploads its odometer to the Volvo cloud after it has been locked for a while, so at drive end the API often still returns the start value. `EndDriveUseCase` then sets `Drive.kmPending = true` (end km null or equal to start km) without bothering the user. The pending end km is filled in silently: by `StartDriveUseCase` (the next drive's start km *is* the previous drive's end km), or at app start by `DriveViewModel` from the current Volvo odometer if it differs from start km. Only when a next drive already exists and the end km still can't be determined does `PendingKmDialog` appear, offering: confirm/edit end km (prefilled via `GetPendingKmDriveUseCase` with the next drive's start km, or else the current Volvo odometer if it differs from start km), re-sync, delete, or postpone until the next app start. `ResolvePendingKmUseCase` clears the flag and recomputes the next drive's mismatch. Pending drives count as unknown km in totals and get "EINDSTAND ONBEKEND" in the CSV.

**Start km & Volvo status**: "Start rit" first fetches the odometer via `FetchStartKmUseCase` (`StartKmDialog` shows a spinner). On success the drive starts with that value; on failure the dialog shows the reason (`OdometerResult.Failure`) and a greyed-out suggestion (previous drive's end km, else last known km) that becomes editable via the pen icon. `VolvoRepository.status` (`VolvoStatus`) is updated on every `fetchOdometer()` call and shown by `VolvoStatusBanner` at the top of the drive list; the app checks it on start.

**Km mismatch detection**: When `StartDriveUseCase` runs, `startKm` (from Volvo API) is compared to the previous drive's `endKm` (skipped if that drive is still `kmPending`). If they differ, `Drive.hasKmMismatch = true`, which turns the card red in the list and adds a "KM MISMATCH" column in the CSV export.

**Database migrations**: `KmDatabase` is at version 5 (`MIGRATION_2_3` adds `kmPending`, `MIGRATION_3_4` adds the `projects` table and `drives.projectId`/`drives.note`, `MIGRATION_4_5` adds `projects.client/lat/lng/address`). `fallbackToDestructiveMigration()` was deliberately removed: a missing migration must crash, never wipe the user's drives. Every schema bump needs an explicit `Migration`; back up the phone's DB (`adb exec-out run-as com.rykle.kmregistratie cat databases/km_registratie.db`, plus `-wal`/`-shm`) before installing one, and always install with `adb install -r` (never uninstall).

**Projects**: a drive can be linked to a `Project` (chosen in the start dialog or on the detail screen) and carry a free-text `note`. `OverviewScreen` shows per-year totals, km per month, the 500 km private-use meter (`DriveStats.PRIVATE_KM_LIMIT`) and km/driving time per project; all totals come from `DriveStats`. Linking a drive to a project only sets `projectId`; odometer values are never touched or redistributed (the user's hard requirement). A project can have a location (saved from a drive's destination or origin via "Locatie opslaan als project"); `ProjectLocations` matches drives that start or end within 150 m (or on the same street+number+city when GPS is missing), offers them in `NearbyDrivesDialog`, and shows a one-tap "Hoort bij …? Koppelen" suggestion on unlinked drive cards. Endpoints within 75 m of home (`ProjectLocations.HOME_RADIUS_METERS`; GPS jitter at home produces neighbours' addresses up to ~60 m away) never match a project; home is set in Settings (`LocationHistoryRepository.get/setHomeLocation`). Rejected suggestions ("Nee", or unchecked in `NearbyDrivesDialog`) are stored per drive+project in `SuggestionPreferences`, outside the database.

**CSV & Nextcloud**: `DriveCsv` is the single CSV format (export, Nextcloud, import); it reads by column name so older exports still import. `ImportDrivesUseCase` only adds drives, skipping any with the same start second; it never deletes or overwrites. `NextcloudSyncUseCase` PUTs `km_ritten.csv` via WebDAV after each drive end and on demand. If the remote file holds drives missing locally, it first saves that file as `km_ritten_<timestamp>.csv`. Credentials live in `AppPreferences` (`nextcloud_*` keys).

**Volvo API authentication**: The access token is stored in `EncryptedSharedPreferences` via `AppPreferences`. The user pastes it manually in the Settings screen. `VolvoRepositoryImpl` sends it as `Authorization: Bearer <token>` with a `VCC-Api-Key` header. The Volvo Connected Vehicle API endpoint is `GET /connected-vehicle/v2/vehicles/{vin}/odometer`.

**Location fallback**: If `FusedLocationProviderClient` fails or permission is not granted, coordinates are stored as `0.0, 0.0`. The `@SuppressLint("MissingPermission")` annotation is present; runtime permission requests are not yet implemented in the UI.

## Tech Stack

- Kotlin 2.0.21, AGP 8.5.2, minSdk 26, targetSdk 35
- Jetpack Compose with Material3
- Room 2.6.1, Hilt 2.52, Retrofit 2.11.0 + Gson
- Play Services Location 21.3.0 (FusedLocationProviderClient)
- Security-Crypto 1.0.0 (EncryptedSharedPreferences for token storage)
