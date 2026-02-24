# Go4O Lite

An Android app for reading and evaluating [SportIdent](https://www.sportident.com/) (SI) electronic punch cards at orienteering events. Connect an SI station via USB, import your courses, and get instant pass/fail results with audio and visual feedback.

## Features

- **SI card readout** — Reads SI5, SI6, and SI8 cards via USB-connected SI stations
- **Course evaluation** — Automatically checks punches against loaded courses, identifies missing controls, and calculates running time
- **IOF XML import** — Load courses from IOF XML v2.0 and v3.0 files
- **Real-time feedback** — Full-screen overlay (text or emoji) with audio tones for pass/fail
- **Readout history** — Browse all card reads with timestamps and results
- **Multi-language** — English, Deutsch, Français, Nederlands, Español, Italiano, Čeština — switchable at runtime

## Supported USB Hardware

| Chip | Vendor ID | Product ID | Typical Use |
|------|-----------|------------|-------------|
| CP2102 | 4292 | 32778 | BSM8-USB, BSF8-USB |
| CP2104 | 4292 | 60000 | Newer SI stations |
| FT232R | 1027 | 24577 | BSF7-USB, older SI stations |
| PL2303 | 1659 | 8963 | SI-USB adapters |
| CH340 | 6790 | 29987 | SI-USB adapters |

Requires an Android device with USB Host support (OTG).

## Requirements

- Android 5.0+ (API 21)
- USB Host (OTG) capability

## Building

```bash
./gradlew assembleDebug       # Build debug APK
./gradlew assembleRelease     # Build release APK (requires keystore.properties)
./gradlew installDebug        # Install on connected device
```

Release signing requires a `keystore.properties` file at the project root (not checked in):

```properties
storeFile=keystore/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Testing

```bash
./gradlew test                # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests (requires device/emulator)
```

## Tech Stack

- Kotlin 1.9, Jetpack Compose, Material 3
- [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) for USB serial communication
- [gecosi](https://github.com/sdenier/GecoSI) SI protocol library (bundled)
- GSON for data persistence via SharedPreferences
- Firebase App Distribution for beta releases

## License

All rights reserved.
