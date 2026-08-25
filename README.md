# Remindy for Android

Kotlin/Jetpack Compose port of the iOS [Remindy](../Remindy) app — a super-minimal,
local-first reminder app driven by NFC tags and places.

- **Local-first**: all data stored on-device via Room. No network, no accounts.
- **NFC-driven**: link a compatible, writable NDEF tag (such as many MIFARE/NTAG
  stickers) to a reminder. The
  app writes a `remindy://t/<uid>` URI record onto the tag.
- Tap your phone on a linked tag → the reminder completes, haptic fires, toast shows.
  When the screen is awake and unlocked, background taps launch the app via
  `NDEF_DISCOVERED`, even if Remindy was closed.
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
| NDEF URI write + read-only handling | `Ndef.writeNdefMessage` with same error messages |
| `remindy://t/` URL scheme | Deep link + `NDEF_DISCOVERED` intent filters |
| CLCircularRegion monitoring (max 20) | `LocationManager.addProximityAlert` (max 20) |
| Time-sensitive notifications | High-importance channel + `CATEGORY_REMINDER` |
| MapKit place picker w/ search & reverse geocoding | osmdroid (OpenStreetMap) + `Geocoder` search/reverse geocoding |
| TipKit history tip | Omitted (platform-specific) |

## Requirements

- Android Studio (current) or JDK 17+
- Android SDK platform 36, build-tools 36.0.0 (`sdkmanager` handles this)
- A **physical device** for NFC reading/writing (emulators have no NFC)

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

1. `+` → name the reminder → optionally *Link NFC Tag* → Add.
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
├── nfc/                     # NfcScanner (reader mode + NDEF write), Haptics
├── location/                # ProximityStore (LocationManager alerts), ProximityReceiver
├── notifications/           # channel + place-alarm notifications
└── ui/
    ├── tasks/               # list screen, rows, swipe/context actions, toasts
    ├── detail/              # create/edit sheet (due date, place, NFC, subtasks)
    ├── history/             # activity calendar sheet
    ├── place/               # Google Maps place picker
    └── theme/
```
