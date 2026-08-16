import UIKit
import WebKit

final class DevicePlugin: WefterPlugin {

    public required init(dispatcher: BridgeDispatcher, viewController: UIViewController?) {
        super.init(dispatcher: dispatcher, viewController: viewController)
        
        UIDevice.current.isBatteryMonitoringEnabled = true
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(batteryStateDidChange),
            name: UIDevice.batteryStateDidChangeNotification,
            object: nil
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc private func batteryStateDidChange(_ notification: Notification) {
        let level = UIDevice.current.batteryLevel
        let state = UIDevice.current.batteryState
        let batteryLevel = level >= 0 ? Double(level) : -1.0
        let isCharging = (state == .charging || state == .full)

        emit("device:chargingChanged", [
            "batteryLevel": batteryLevel,
            "isCharging": isCharging
        ])
    }

    // @WefterMethod
    func getInfo(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let isSimulator: Bool
        #if targetEnvironment(simulator)
        isSimulator = true
        #else
        isSimulator = false
        #endif

        var info: [String: Any] = [
            "model": UIDevice.current.model,
            "platform": "ios",
            "operatingSystem": "ios",
            "osVersion": UIDevice.current.systemVersion,
            "manufacturer": "Apple",
            "isVirtual": isSimulator
        ]

        DispatchQueue.main.async {
            guard let webView = self.dispatcher.currentWebView else {
                info["webViewVersion"] = ""
                self.resolve(callback, data: info)
                return
            }
            webView.evaluateJavaScript("navigator.userAgent") { result, _ in
                info["webViewVersion"] = (result as? String) ?? ""
                self.resolve(callback, data: info)
            }
        }
    }

    // @WefterMethod
    func getId(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let uuid = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        resolve(callback, data: ["uuid": uuid])
    }

    // @WefterMethod
    func getBatteryInfo(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let level = UIDevice.current.batteryLevel
        let state = UIDevice.current.batteryState

        let batteryLevel = level >= 0 ? Double(level) : -1.0
        let isCharging = (state == .charging || state == .full)

        resolve(callback, data: [
            "batteryLevel": batteryLevel,
            "isCharging": isCharging
        ])
    }

    // @WefterMethod
    func getLanguageCode(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let languageCode = Locale.preferredLanguages.first ?? Locale.current.identifier
        resolve(callback, data: ["value": languageCode])
    }
}
