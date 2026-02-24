# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Go4O Lite is an Android app for reading and evaluating SportIdent (SI) electronic punch cards at orienteering events. It connects to SI stations via USB, evaluates punches against loaded courses, and displays real-time results with audio/visual feedback.

- Package: `com.go4o.lite`
- Min SDK 21 / Target SDK 34
- Kotlin 1.9.22, Jetpack Compose (BOM 2024.01.00), Material 3

## Build Commands

```bash
./gradlew build                    # Full build
./gradlew assembleDebug            # Debug APK
./gradlew assembleRelease          # Release APK (requires keystore.properties)
./gradlew installDebug             # Install debug APK on connected device
./gradlew test                     # Unit tests (JUnit 4)
./gradlew connectedAndroidTest     # Instrumented tests (requires device/emulator)
./gradlew test --tests "com.go4o.lite.domain.CourseEvaluatorTest"  # Single test class
```

## Architecture

Single-activity MVVM app using Jetpack Compose. No DI framework — dependencies are manually instantiated.

```
app/src/main/java/com/go4o/lite/
├── MainActivity.kt              # Entry point, USB device handling
├── ui/
│   ├── screens/MainScreen.kt    # Main Compose UI with 4 tabs (Home, Courses, Readouts, Settings)
│   ├── viewmodel/MainViewModel.kt  # Central state via MutableStateFlow<MainUiState>
│   ├── theme/                   # Material 3 theming
│   ├── strings/Strings.kt      # All UI strings for 7 languages (EN, DE, FR, NL, ES, IT, CS)
│   └── SoundPlayer.kt          # Audio feedback for pass/fail
├── domain/
│   ├── CourseEvaluator.kt       # Evaluates SI card punches against courses
│   └── SiDataFrameConverter.kt  # Converts SI protocol frames to domain models
├── data/
│   ├── model/                   # Data classes: SiCardResult, Course, ReadoutResult, AppSettings
│   ├── persistence/AppDataStore.kt  # SharedPreferences + GSON serialization
│   └── xml/IofXmlParser.kt     # IOF XML v2.0 and v3.0 course import
└── si/
    ├── SiStationManager.kt      # USB device discovery, connection, permissions
    └── adapter/                 # USB serial communication (UsbSiPort, reader/writer)
```

Bundled Java libraries under `net.gecosi.*` and `de.sportident.*` implement the SI protocol and CRC calculation.

## Key Patterns

- **State management**: Single `MainUiState` data class exposed as `StateFlow` from `MainViewModel`
- **Persistence**: SharedPreferences (`go4o_lite_data`) with GSON for complex types — no Room/SQLite
- **SI card formats**: Supports SI5, SI6, SI8 via the gecosi library
- **USB drivers**: CP2102, CP2104, FT232R, PL2303, CH340 — configured in `res/xml/usb_device_filter.xml`
- **Localization**: Runtime language switching via `Strings.kt` object (not Android resource system)
- **Domain objects**: Immutable data classes (CourseEvaluator returns `EvaluationResult` with PASS/FAIL/NO_COURSE status)

## Testing

Unit tests are in `app/src/test/` — key test files:
- `CourseEvaluatorTest.kt` — punch evaluation scenarios, missing controls, time calculation
- `IofXmlParserTest.kt` — XML parsing for both IOF formats

## Build Configuration Notes

- Java 1.8 target compatibility
- ProGuard/minification disabled in release builds
- Release signing requires `keystore.properties` at project root (not in version control)
- JitPack repository used for `usb-serial-for-android` library
- Firebase App Distribution configured for beta releases
