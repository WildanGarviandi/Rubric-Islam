# Rubric Islam

An Android prayer times and Qiblat direction app built with **Kotlin + Jetpack Compose + Material 3**, featuring clean architecture with **Hilt DI**, **Room**, and **WorkManager**.

![Platform](https://img.shields.io/badge/Platform-Android-green) ![Language](https://img.shields.io/badge/Language-Kotlin-blue) ![Architecture](https://img.shields.io/badge/Architecture-Clean%20Architecture-yellow)

---

## Features

### Prayer Times

- **Location-based prayer times** calculated in real-time using the [Aladhan API](https://aladhan.com)
- **Automatic schedule** via background `WorkManager` — reminder is pushed every prayer session on your actual location
- **No manual refresh needed** — times are scheduled locally with device clock as the source of truth
- Persists prayer times in **Room database** for offline access

### Qiblat Direction

- **Compass + Kaaba visualization** that rotates to show the exact direction to Makkah
- **Manual orientation mode** — let users tap their direction of prayer without requiring camera/GPS permission
- Smooth animations via Jetpack Compose

### Reminders & Alerts

- Alarm-style notifications for each prayer call (Fajr, Dhuhr, Asr, Maghrib, Isha)
- Configurable per-device with WorkManager
- Scheduler checks device time to only fire remaining prayers of the day

---

## Architecture

The project follows **clean architecture** with these layers:

```
ui/              (Compose screens + ViewModels)
  └── prayer/     PrayerTimesScreen, PrayerTimeViewModel, CelestialPrayerChart, ...
  └── qiblat/     QiblatScreen, QiblatViewModel, ModernCompassView
  └── reminder/   ReminderScreen
  └── navigation/ NavGraph, Screen enum

domain/          (use cases, models, repository interfaces)
  ├── usecase/    GetPrayerTimesUseCase, GetQiblatDirectionUseCase, GetLocationUseCase
  ├── model/      PrayerTime, Qiblat
  └── repository/ PrayerTimeRepository, QiblatRepository, ReminderRepository

data/            (DTOs, API, local DB, repository impl)
  ├── remote/     AladhanApiService, AladhanDto
  ├── local/      Room DB, DAO, entities
  └── repository/ PrayerTimeRepositoryImpl, ...

di/              (Hilt modules)
worker/          (PrayerTimeWorker, AlarmReceiver)
```

---

## How to Build & Run

### Prerequisites

- **Android Studio Hedgehog** (or later)
- **JDK 17+**
- **Gradle 8+**
- **Android SDK 37** (compileSdk)

### Build

```bash
./gradlew :app:assembleDebug
```

### Run on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Design

The UI uses a **dark theme** inspired by desert night skies:

- `MidnightIndigo` / `TwilightBlue` / `DeepSapphire` — sky palette
- `DesertGold` / `SoftGold` / `MutedGold` — lantern accents
- `OasisEmerald` / `SageGlass` — natural accents

---

## License

Copyright (c) 2026 Kellin Reaver. All rights reserved.
