# @wefterjs/device

Official Wefter plugin for querying device metadata, unique ID, battery state, and system locale on Android and iOS.

---

## Features

- 📱 **Device Metadata**: Fetch model, OS version, manufacturer, and emulator detection with `getInfo()`.
- 🔑 **Device Identifier**: Retrieve unique device vendor/hardware UUID with `getId()`.
- 🔋 **Battery Info**: Access battery level (0.0 to 1.0) and charging status with `getBatteryInfo()`.
- 🌐 **Locale & Language**: Read active system language code with `getLanguageCode()`.

---

## Installation & Setup

1. Add the plugin to your Wefter project:

```bash
wefter add @wefterjs/device
```

2. Synchronize native projects:

```bash
wefter sync
```

---

## JavaScript / TypeScript API Reference

```ts
import { Device } from "@wefterjs/device";
```

### 1. `getInfo()`

Returns general device hardware and operating system metadata.

```ts
const info = await Device.getInfo();
console.log(info.model, info.osVersion, info.isVirtual);
```

### 2. `getId()`

Returns the unique device identifier UUID.

```ts
const { uuid } = await Device.getId();
console.log("Device UUID:", uuid);
```

### 3. `getBatteryInfo()`

Returns battery level percentage and charging status.

```ts
const battery = await Device.getBatteryInfo();
console.log(`Battery: ${battery.batteryLevel * 100}%, Charging: ${battery.isCharging}`);
```

### 4. `getLanguageCode()`

Returns the primary language code tag (e.g. `en-US`).

```ts
const lang = await Device.getLanguageCode();
console.log("System language:", lang.value);
```

---

## Complete Usage Example

```ts
import { Device } from "@wefterjs/device";

export async function logDiagnostics() {
  const info = await Device.getInfo();
  const { uuid } = await Device.getId();
  const battery = await Device.getBatteryInfo();
  const lang = await Device.getLanguageCode();

  console.log("Diagnostics:", {
    device: `${info.manufacturer} ${info.model} (OS ${info.osVersion})`,
    uuid,
    battery: `${(battery.batteryLevel * 100).toFixed(0)}% (${battery.isCharging ? "Charging" : "Discharging"})`,
    language: lang.value,
  });
}
```
