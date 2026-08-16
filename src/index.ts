import { definePlugin, registerHook } from "@wefterjs/core";

export interface DeviceInfo {
  model: string;
  platform: "android" | "ios" | "web";
  operatingSystem: "android" | "ios" | "mac" | "windows" | "linux" | "unknown" | string;
  osVersion: string;
  manufacturer: string;
  isVirtual: boolean;
  webViewVersion?: string;
}

export interface DeviceId {
  uuid: string;
}

export interface BatteryInfo {
  batteryLevel: number;
  isCharging: boolean;
}

export interface LanguageCode {
  value: string;
}

const NativeDevice = definePlugin<{
  getInfo: () => Promise<DeviceInfo>;
  getId: () => Promise<DeviceId>;
  getBatteryInfo: () => Promise<BatteryInfo>;
  getLanguageCode: () => Promise<LanguageCode>;
}>("device", {
  getInfo: true,
  getId: true,
  getBatteryInfo: true,
  getLanguageCode: true,
});

export const Device = {
  ...NativeDevice,
  onChargingChange(callback: (status: BatteryInfo) => void): { remove(): void } {
    return registerHook("device:chargingChanged", callback as (data: unknown) => void);
  },
};
