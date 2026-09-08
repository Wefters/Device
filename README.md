# @wefterjs/device

Official Wefter plugin for querying device metadata, hardware identifiers, battery status, charging updates, and system language locale on Android and iOS.

## Features

- Query device model, manufacturer, operating system version, and virtual emulator status.
- Retrieve a persistent unique vendor UUID.
- Inspect battery charge levels and charging states.
- Subscribe to real-time charging status change events.
- Retrieve active system language codes.
- No runtime permissions required.

## Installation and setup

Install the plugin package in your Wefter application:

```bash
wefter add @wefterjs/device
wefter sync
```

## JavaScript API reference

Import `Device` from `@wefterjs/device`:

```ts
import { Device } from "@wefterjs/device";
```

### Device metadata

Retrieve hardware and operating system details:

```ts
const info = await Device.getInfo();

console.log("Model:", info.model);                   // e.g. "Pixel 8" or "iPhone 15,2"
console.log("Platform:", info.platform);             // "android", "ios", or "web"
console.log("Operating system:", info.operatingSystem); // "android" or "ios"
console.log("OS version:", info.osVersion);         // e.g. "14" or "17.4"
console.log("Manufacturer:", info.manufacturer);     // e.g. "Google" or "Apple"
console.log("Is virtual/emulator:", info.isVirtual); // true when running in emulator or simulator
if (info.webViewVersion) {
  console.log("WebView version:", info.webViewVersion);
}
```

### Unique device identifier

Retrieve the unique vendor hardware identifier:

```ts
const { uuid } = await Device.getId();
console.log("Device UUID:", uuid);
```

On Android, this maps to `Settings.Secure.ANDROID_ID`. On iOS, this maps to `UIDevice.current.identifierForVendor`.

### Battery information and charging events

Inspect current battery status and monitor charging transitions:

```ts
// Check battery level and charging state
const battery = await Device.getBatteryInfo();
console.log("Battery level:", Math.round(battery.batteryLevel * 100) + "%");
console.log("Is charging:", battery.isCharging);

// Subscribe to charging transitions
const sub = Device.onChargingChange((status) => {
  console.log("Charging state updated:", status.isCharging);
  console.log("Current level:", status.batteryLevel);
});

// Remove listener when done
sub.remove();
```

### System language

Retrieve the user's preferred system language locale:

```ts
const lang = await Device.getLanguageCode();
console.log("Language code:", lang.value); // e.g. "en-US"
```

## Complete usage example

```ts
import { Device } from "@wefterjs/device";

export async function collectDeviceDiagnostics() {
  const [info, id, battery, lang] = await Promise.all([
    Device.getInfo(),
    Device.getId(),
    Device.getBatteryInfo(),
    Device.getLanguageCode(),
  ]);

  return {
    device: `${info.manufacturer} ${info.model}`,
    os: `${info.operatingSystem} ${info.osVersion}`,
    isSimulator: info.isVirtual,
    deviceId: id.uuid,
    batteryPercentage: Math.round(battery.batteryLevel * 100),
    isCharging: battery.isCharging,
    locale: lang.value,
  };
}
```

## Platform implementation notes

### Android

- **Metadata**: Uses `android.os.Build` properties.
- **Battery**: Queries `BatteryManager` and registers a `BroadcastReceiver` for `ACTION_BATTERY_CHANGED`.
- **Identifier**: Uses `Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)`.
- **Locale**: Reads `Locale.getDefault().toLanguageTag()`.

### iOS

- **Metadata**: Queries `UIDevice.current` and `sysctlbyname` for model machine codes.
- **Battery**: Enables `isBatteryMonitoringEnabled` on `UIDevice.current` and listens to `batteryStateDidChangeNotification`.
- **Identifier**: Uses `UIDevice.current.identifierForVendor?.uuidString`.
- **Locale**: Reads `Locale.preferredLanguages.first`.

## License

[MIT](LICENSE) © 2026 Sandip Ghimire
