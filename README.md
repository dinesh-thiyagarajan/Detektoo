# Detekto

A utility Android app that detects and displays cellular signal strength from all available network providers at your current location. Compare signal quality across carriers in real-time with a graphical bar chart and detailed per-provider cards.

## Screenshots

| Signal List | Bar Chart Comparison | Permission Prompt |
|:-----------:|:--------------------:|:-----------------:|
| ![Signal List](screenshots/signal_list.png) | ![Bar Chart](screenshots/bar_chart.png) | ![Permission](screenshots/permission.png) |

| Loading State | No Signal State | With Ads |
|:-------------:|:---------------:|:--------:|
| ![Loading](screenshots/loading.png) | ![No Signal](screenshots/no_signal.png) | ![Ads](screenshots/with_ads.png) |

> **Note:** Add screenshots to a `screenshots/` directory in the project root.

## Features

- **Multi-provider detection** — Scans all visible cell towers and displays every operator your device can see (Jio, Airtel, Vodafone, BSNL, etc.)
- **Signal strength percentage** — Translates raw dBm readings into an easy-to-read 0–100% scale
- **Graphical bar chart** — Side-by-side comparison of all detected providers with color-coded bars
- **Network type identification** — Shows whether each signal is LTE, GSM, WCDMA, or 5G NR
- **Live updates** — Signal data refreshes automatically every 3 seconds
- **Connected indicator** — Highlights which provider your device is currently registered to
- **AdMob integration** — Banner ads with a build-time toggle to enable or disable ads entirely
- **Material 3 UI** — Built with Jetpack Compose and Material You theming

## Architecture

The project follows **MVVM Clean Architecture** with a **multi-module** structure:

```
detekto/
├── app/                    # Application entry point, DI setup
├── core/                   # Shared utilities (AdMob wrapper, theme colors, BuildConfig)
└── signal/                 # Self-contained feature module
    ├── data/               #   Repository implementations, DI
    ├── domain/             #   Entities, use cases, repository interfaces
    └── ui/                 #   ViewModel, Compose screens, graph
```

Each feature module is self-contained with its own `data`, `domain`, and `ui` packages. No separate `:data` or `:domain` modules.

### Module Dependency Graph

```
app
├── core
└── signal ──► core
```

### Layer Responsibilities

| Layer | Package | Contents |
|-------|---------|----------|
| **Presentation** | `signal/ui` | `SignalViewModel`, Compose screens, bar chart component |
| **Domain** | `signal/domain` | `SignalInfo` entity, `SignalRepository` interface, `GetSignalStrengthUseCase` |
| **Data** | `signal/data` | `SignalRepositoryImpl` (reads `TelephonyManager`), Hilt `SignalDataModule` |
| **Core** | `core` | `AdManager`, `BannerAdView` composable, theme colors, `BuildConfig.SHOW_ADS` |

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt (Dagger)
- **Async:** Kotlin Coroutines + Flow
- **Build:** Gradle Version Catalog (`libs.versions.toml`)
- **Ads:** Google AdMob
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36

## Getting Started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 11+
- An Android device with a SIM card (emulators have limited cell info)

### Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/your-username/detekto.git
   cd detekto
   ```

2. **Configure ads (optional)**

   Open `local.properties` and set the ad toggle:

   ```properties
   # Set to true to show ads, false to hide them
   SHOW_ADS=true
   ```

   For production, replace the test ad unit IDs in `core/.../BannerAd.kt` and the application ID in `app/AndroidManifest.xml` with your real AdMob IDs.

3. **Build and run**

   ```bash
   ./gradlew assembleDebug
   ```

   Or open the project in Android Studio and run the `app` configuration.

### Permissions

The app requests these permissions at runtime:

| Permission | Reason |
|------------|--------|
| `ACCESS_FINE_LOCATION` | Required by Android to access cell tower information via `TelephonyManager.allCellInfo` |
| `READ_PHONE_STATE` | Allows reading telephony state for signal details |
| `INTERNET` | Required for loading AdMob ads |
| `ACCESS_NETWORK_STATE` | Used by AdMob to check connectivity |

## How It Works

1. On launch, the app requests location and phone state permissions
2. `SignalRepositoryImpl` polls `TelephonyManager.getAllCellInfo()` every 3 seconds
3. Each `CellInfo` object (LTE, GSM, WCDMA, NR) is parsed into a `SignalInfo` domain model
4. The signal level (0–4) is converted to a percentage (0–100%)
5. The `SignalViewModel` collects the flow and exposes a `SignalUiState` to the UI
6. The UI renders a bar chart for visual comparison and individual cards for each provider

## Ad Configuration

Ads are controlled by a **build-time boolean** in `local.properties`:

```properties
# Enable ads
SHOW_ADS=true

# Disable ads
SHOW_ADS=false
```

This value is read during the Gradle build and injected into `core` module's `BuildConfig.SHOW_ADS`. The `AdManager` object exposes this flag, and `BannerAdView` checks it before rendering. When `SHOW_ADS=false`, no ad SDK calls are made and no ad views are inflated.

**Current ad unit IDs are Google's test IDs.** Replace them before publishing:

| Location | Current Value | Replace With |
|----------|---------------|--------------|
| `app/AndroidManifest.xml` | `ca-app-pub-3940256099942544~3347511713` | Your AdMob App ID |
| `core/.../BannerAd.kt` | `ca-app-pub-3940256099942544/6300978111` | Your Banner Ad Unit ID |

## Project Configuration

### Version Catalog

All dependencies are managed via `gradle/libs.versions.toml`. Key versions:

| Dependency | Version |
|------------|---------|
| AGP | 8.13.2 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.00 |
| Hilt | 2.51.1 |
| AdMob | 23.6.0 |
| KSP | 2.0.21-1.0.28 |

### Git Ignore

The `.gitignore` is configured to exclude:

- `.idea/` directory (IDE settings)
- All `build/` directories
- `local.properties` (SDK path + ad config)
- Gradle caches (`.gradle/`)
- Keystore files (`*.jks`, `*.keystore`)
- macOS `.DS_Store` files

## License

This project is available under the [MIT License](LICENSE).
