# Remindy for Android

Kotlin/Jetpack Compose port of the iOS [Remindy](../Remindy) app — a super-minimal,
local-first reminder app driven by NFC tags and places.

- **Local-first**: all data stored on-device via Room. No network, no accounts.
- **NFC-driven**: after an explicit overwrite warning, Remindy replaces the tag's NDEF
  contents with a `remindy://t/<uid>` link tied to the reminder.
- Tap a linked tag to open Remindy and complete its reminder, or scan it from the NFC
  button inside the app.
- **Place reminders**: native `LocationManager` proximity alerts (entry/exit,
  50–500 m radius, max 20 regions) with high-priority notifications even when the
  app is closed.
- **No Google Play Services required** — geolocation uses the framework
  `LocationManager`, and the place picker uses OpenStreetMap via osmdroid.

## Feature parity with iOS

| iOS | Android |
| --- | --- |
| SwiftData `Reminder` model | Room `ReminderEntity` (same fields incl. log history, subtasks via self-FK cascade) |
| Recurrence none/daily/weekly/monthly | Same, wall-clock-safe via `java.time` |
| Logger mode ("Auto-Reset After Log") | Same |
| CoreNFC reader session | `NfcAdapter.enableReaderMode` (A/B/F polling) |
| NFC link writing | `NfcAdapter` reader mode + NDEF overwrite/formatting |
| Legacy `remindy://t/` URL scheme | Deep link + `NDEF_DISCOVERED` intent filters |
| CLCircularRegion monitoring (max 20) | `LocationManager.addProximityAlert` (max 20) |
| Time-sensitive notifications | High-importance channel + `CATEGORY_REMINDER` |
| MapKit place picker w/ search & reverse geocoding | osmdroid (OpenStreetMap) + `Geocoder` search/reverse geocoding |
| TipKit history tip | Omitted (platform-specific) |

## Requirements

- Android Studio (current) or JDK 17+
- Android SDK platform 36, build-tools 36.0.0 (`sdkmanager` handles this)
- A **physical device** for NFC reading (emulators have no NFC)

## Setup

Set your SDK path in `local.properties`:

```
sdk.dir=/path/to/android-sdk
```

No API keys needed — maps are OpenStreetMap and location is framework-native.

## Build

```sh
./gradlew :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run.

## Usage

1. `+` → name the reminder → optionally *Link NFC Tag* → confirm overwrite → Add.
2. NFC icon (top-left) → hold phone near any linked tag → matching reminder completes.
3. Tapping an unlinked tag offers nothing special in-app; link tags from the edit sheet.
4. Checkmark toggles today manually; swipe left to delete; long-press for
   Edit / Complete / Archive / Delete.

## Structure

```
app/src/main/java/com/francescooddo/remindy/
├── MainActivity.kt          # entry + deep-link handling (remindy://t/<uid>)
├── Graph.kt                 # tiny object graph (db + geofence store)
├── data/                    # Room entity, DAO, database, converters
├── domain/                  # Recurrence, PlaceTrigger, completion logic ports
├── nfc/                     # NfcScanner (UID reads + NDEF link writes), Haptics
├── location/                # ProximityStore (LocationManager alerts), ProximityReceiver
├── notifications/           # channel + place-alarm notifications
└── ui/
    ├── tasks/               # list screen, rows, swipe/context actions, toasts
    ├── detail/              # create/edit sheet (due date, place, NFC, subtasks)
    ├── history/             # activity calendar sheet
    ├── place/               # Google Maps place picker
    └── theme/
```
