package dev.wefter.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import java.util.Locale
import java.util.UUID
import org.json.JSONObject

class DevicePlugin(context: Context, dispatcher: BridgeDispatcher) :
        WefterPlugin(context, dispatcher) {

    // ---------------------------------------------------------------------------
    // Battery charging event
    // ---------------------------------------------------------------------------

    /**
     * Receives ACTION_POWER_CONNECTED and ACTION_POWER_DISCONNECTED to emit
     * a "device:chargingChanged" hook whenever the charger is plugged or unplugged.
     * ACTION_BATTERY_CHANGED is also included so level updates on charging state
     * transitions are captured in a single snapshot.
     */
    private val chargingReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    try {
                        val batteryJson = readBatteryJson(ctx ?: context)
                        emit("device:chargingChanged", batteryJson)
                    } catch (e: Exception) {
                        // Silently ignore — JS listeners should not crash the app
                    }
                }
            }

    init {
        try {
            val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_POWER_CONNECTED)
                        addAction(Intent.ACTION_POWER_DISCONNECTED)
                    }
            context.registerReceiver(chargingReceiver, filter)
        } catch (e: Exception) {
            // Registration may fail in restricted contexts; non-fatal
        }
    }

    // ---------------------------------------------------------------------------
    // @WefterMethod implementations
    // ---------------------------------------------------------------------------

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

    @WefterMethod
    fun getBatteryInfo(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        try {
            resolve(callback, readBatteryJson(context))
        } catch (e: Exception) {
            reject(callback, "BATTERY_FAILED", e.message ?: "Failed to get battery info.")
        }
    }

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

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Reads a battery snapshot from a sticky ACTION_BATTERY_CHANGED broadcast. */
    private fun readBatteryJson(ctx: Context): JSONObject {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = ctx.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryLevel =
                if (level >= 0 && scale > 0) {
                    (level.toFloat() / scale.toFloat())
                } else {
                    -1.0f
                }

        val isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        return JSONObject()
                .put("batteryLevel", batteryLevel.toDouble())
                .put("isCharging", isCharging)
    }
}
