# 🅿️ ParkWatch

**Automatic parking countdown timer triggered by Bluetooth car disconnect.**

ParkWatch monitors your car's Bluetooth connection. When you park and disconnect from your car in a town centre during parking restriction hours, it automatically starts a countdown timer and reminds you when time is nearly up.

## Features

- 🔵 **Bluetooth detection** — monitors your car's specific Bluetooth device
- ⏱️ **Auto-start timer** — starts automatically when you disconnect from your car
- 🗺️ **Location zone check** — only activates in your configured town centre zone
- 🕐 **Restriction hours** — only runs during configurable parking restriction hours
- 📱 **Live notification** — persistent countdown in your notification shade
- ⚠️ **Expiry alert** — loud notification when time runs out
- 🚗 **Auto-cancel** — cancels timer when you reconnect to your car
- 📋 **History** — log of recent parking sessions

## Default Configuration

| Setting | Default |
|---|---|
| Timer duration | 90 minutes |
| Zone | Huddersfield town centre (lat: 53.6458, lng: -1.7850, radius: 600m) |
| Weekday hours | Mon–Sat 08:00–18:00 |
| Sunday hours | 12:00–18:00 |

All defaults are configurable in the Settings screen.

## Setup

1. Install the app
2. Grant the requested permissions (Bluetooth, Location, Notifications)
3. Go to **Settings** and select your car's Bluetooth device from the paired devices list
4. Optionally adjust the zone, timer duration, and restriction hours
5. That's it — ParkWatch runs silently in the background

## How to Build

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK (API 34)

### Steps

```bash
git clone https://github.com/beezly/parkwatch.git
cd parkwatch
./gradlew assembleDebug
```

Or open in Android Studio and click **Run**.

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Install via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Tech Stack

- **Kotlin** — 100% Kotlin, no Java
- **Jetpack Compose + Material 3** — modern declarative UI
- **DataStore** — settings persistence
- **Foreground Service** — timer survives screen off
- **FusedLocationProviderClient** — efficient location checks
- **BroadcastReceiver** — Bluetooth event monitoring

## Requirements

- Android 8.0 (API 26) or higher
- Bluetooth (for car detection)
- Location permission (optional, for zone check)
- Notification permission (Android 13+)

## Permissions

| Permission | Purpose |
|---|---|
| `BLUETOOTH_CONNECT` | Detect car Bluetooth connection/disconnection |
| `BLUETOOTH_SCAN` | List paired devices in settings |
| `ACCESS_FINE_LOCATION` | Check you're in the parking zone |
| `POST_NOTIFICATIONS` | Show countdown and expiry alerts |
| `FOREGROUND_SERVICE` | Keep timer running in background |

## Licence

MIT — see [LICENSE](LICENSE).
