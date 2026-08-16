// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { installMockBridge, uninstallMockBridge } from "@wefterjs/core/testing";
import { Device } from "../src/index.js";

afterEach(() => {
  uninstallMockBridge();
});

describe("Device.getInfo", () => {
  it("resolves device metadata", async () => {
    installMockBridge({
      device: (method) => {
        expect(method).toBe("getInfo");
        return {
          model: "Pixel 7",
          platform: "android",
          operatingSystem: "android",
          osVersion: "14",
          manufacturer: "Google",
          isVirtual: false,
          webViewVersion: "Chrome/120.0.0.0",
        };
      },
    });

    const info = await Device.getInfo();
    expect(info.model).toBe("Pixel 7");
    expect(info.platform).toBe("android");
    expect(info.operatingSystem).toBe("android");
    expect(info.osVersion).toBe("14");
    expect(info.manufacturer).toBe("Google");
    expect(info.isVirtual).toBe(false);
  });
});

describe("Device.getId", () => {
  it("resolves device UUID", async () => {
    installMockBridge({
      device: (method) => {
        expect(method).toBe("getId");
        return { uuid: "12345678-abcd-1234-abcd-123456789abc" };
      },
    });

    const result = await Device.getId();
    expect(result.uuid).toBe("12345678-abcd-1234-abcd-123456789abc");
  });
});

describe("Device.getBatteryInfo", () => {
  it("resolves battery level and charging status", async () => {
    installMockBridge({
      device: (method) => {
        expect(method).toBe("getBatteryInfo");
        return { batteryLevel: 0.85, isCharging: true };
      },
    });

    const battery = await Device.getBatteryInfo();
    expect(battery.batteryLevel).toBe(0.85);
    expect(battery.isCharging).toBe(true);
  });
});

describe("Device.getLanguageCode", () => {
  it("resolves current language tag/code", async () => {
    installMockBridge({
      device: (method) => {
        expect(method).toBe("getLanguageCode");
        return { value: "en-US" };
      },
    });

    const lang = await Device.getLanguageCode();
    expect(lang.value).toBe("en-US");
  });
});
