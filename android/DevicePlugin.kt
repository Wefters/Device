package dev.wefter.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.UUID
import org.json.JSONObject

/**
 * Device, OS and battery information. Listens to the battery broadcast to report when the device starts or
 * stops charging.
 */
class DevicePlugin(context: Context, dispatcher: BridgeDispatcher) :
        WefterPlugin(context, dispatcher) {

    /** The charging state last reported to JavaScript, so only real changes are emitted. */
    private var lastCharging: Boolean? = null

    /**
     * Emits `device:chargingChanged` when the charging state in a battery broadcast differs from the last
     * one. The state is read from the broadcast itself: reading the sticky battery intent when the charger
     * connects can return the state from before the change.
     */
    private val chargingReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent == null || isInitialStickyBroadcast) return
                    try {
                        val batteryJson = batteryJsonFrom(intent)
                        val charging = batteryJson.getBoolean("isCharging")
                        if (charging == lastCharging) return
                        lastCharging = charging
                        emit("device:chargingChanged", batteryJson)
                    } catch (e: Exception) {
                    }
                }
            }

    /**
     * Starts listening for battery changes, which Android sends when the charger is connected or
     * disconnected (and when the level changes). The current state becomes the baseline.
     */
    init {
        try {
            val current =
                    ContextCompat.registerReceiver(
                            context,
                            chargingReceiver,
                            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                            ContextCompat.RECEIVER_NOT_EXPORTED
                    )
            lastCharging = current?.let { batteryJsonFrom(it).getBoolean("isCharging") }
        } catch (e: Exception) {
        }
    }

    // @WefterMethod implementations

    /**
     * Model, manufacturer, OS version, the system HTTP user agent (as `webViewVersion`), and whether this is
     * an
     * emulator.
     */
    @WefterMethod
    fun getInfo(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        try {
            val isVirtual =
                    Build.FINGERPRINT.startsWith("generic") ||
                            Build.FINGERPRINT.startsWith("unknown") ||
                            Build.MODEL.contains("google_sdk") ||
                            Build.MODEL.contains("Emulator") ||
                            Build.MODEL.contains("Android SDK built for x86") ||
                            Build.MANUFACTURER.contains("Genymotion") ||
                            Build.HARDWARE.contains("goldfish") ||
                            Build.HARDWARE.contains("ranchu")

            val info = JSONObject()
            info.put("model", Build.MODEL ?: "Unknown")
            info.put("platform", "android")
            info.put("operatingSystem", "android")
            info.put("osVersion", Build.VERSION.RELEASE ?: "Unknown")
            info.put("manufacturer", Build.MANUFACTURER ?: "Unknown")
            info.put("isVirtual", isVirtual)
            info.put("webViewVersion", System.getProperty("http.agent") ?: "")

            resolve(callback, info)
        } catch (e: Exception) {
            reject(callback, "INFO_FAILED", e.message ?: "Failed to get device info.")
        }
    }

    /** The Android ID, or a random UUID when it is unavailable. */
    @WefterMethod
    fun getId(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        try {
            val androidId =
                    Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ANDROID_ID
                    )
            val uuid = if (!androidId.isNullOrBlank()) androidId else UUID.randomUUID().toString()
            resolve(callback, JSONObject().put("uuid", uuid))
        } catch (e: Exception) {
            reject(callback, "ID_FAILED", e.message ?: "Failed to get device ID.")
        }
    }

    /** Battery level from 0 to 1 and charging state. */
    @WefterMethod
    fun getBatteryInfo(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        try {
            resolve(callback, readBatteryJson(context))
        } catch (e: Exception) {
            reject(callback, "BATTERY_FAILED", e.message ?: "Failed to get battery info.")
        }
    }

    /** The device language, such as `en`. */
    @WefterMethod
    fun getLanguageCode(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        try {
            val locale = Locale.getDefault()
            val languageCode = locale.toLanguageTag()
            resolve(callback, JSONObject().put("value", languageCode))
        } catch (e: Exception) {
            reject(callback, "LANGUAGE_FAILED", e.message ?: "Failed to get language code.")
        }
    }

    /** Battery level and charging state from the sticky battery broadcast. */
    private fun readBatteryJson(ctx: Context): JSONObject {
        val batteryStatus: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryJsonFrom(batteryStatus)
    }

    /**
     * Battery level from 0 to 1 (`-1` when unknown) and whether the device is charging, from a battery
     * broadcast. Charging means connected to power, as on iOS: the plug state changes with the charger,
     * while the status can lag behind it or read "not charging" when the phone holds the charge (battery
     * protection, or full).
     */
    private fun batteryJsonFrom(batteryStatus: Intent?): JSONObject {
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryLevel = if (level >= 0 && scale > 0) level.toDouble() / scale else -1.0

        val isCharging =
                if (plugged >= 0) {
                    plugged != 0
                } else {
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                }

        return JSONObject()
                .put("batteryLevel", batteryLevel)
                .put("isCharging", isCharging)
    }
}
