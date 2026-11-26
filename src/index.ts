/** Information about the device: model, OS, battery, language and an app-specific identifier. */

import { definePlugin, registerHook } from "@wefterjs/core";

/** Hardware and operating system details. `isVirtual` is true on simulators and emulators. */
export interface DeviceInfo {
  model: string;
  platform: "android" | "ios" | "web";
  operatingSystem: "android" | "ios" | "mac" | "windows" | "linux" | "unknown" | string;
  osVersion: string;
  manufacturer: string;
  isVirtual: boolean;
  webViewVersion?: string;
}

/**
 * An identifier for this device. On iOS it is the identifier for this vendor's apps; on Android the
 * Android ID, which is per app signing key.
 */
export interface DeviceId {
  uuid: string;
}

/** Battery charge from 0 to 1 (`-1` when unknown) and whether it is charging. */
export interface BatteryInfo {
  batteryLevel: number;
  isCharging: boolean;
}

/** The device's preferred language, such as `en`. */
export interface LanguageCode {
  value: string;
}

/** Native methods of the plugin. */
const NativeDevice = definePlugin<{
  /** Hardware and operating system details. */
  getInfo: () => Promise<DeviceInfo>;
  /** An identifier for this device. */
  getId: () => Promise<DeviceId>;
  /** Battery level and charging state. */
  getBatteryInfo: () => Promise<BatteryInfo>;
  /** The preferred language code. */
  getLanguageCode: () => Promise<LanguageCode>;
}>("device", {
  getInfo: true,
  getId: true,
  getBatteryInfo: true,
  getLanguageCode: true,
});

/** The Device plugin. */
export const Device = {
  ...NativeDevice,
  /**
   * Calls `callback` when the device starts or stops charging.
   *
   * @returns A handle whose `remove()` stops listening.
   */
  onChargingChange(callback: (status: BatteryInfo) => void): { remove(): void } {
    return registerHook("device:chargingChanged", callback as (data: unknown) => void);
  },
};
