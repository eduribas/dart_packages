import AuthenticationServices
import Flutter
import UIKit

let methodChannelName = "com.aboutyou.dart_packages.sign_in_with_apple"

public class SwiftSignInWithApplePlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(
            name: methodChannelName,
            binaryMessenger: registrar.messenger()
        )

        if #available(iOS 13.0, macOS 10.15, *) {
            let instance = SignInWithAppleNativePlugin()
            registrar.addMethodCallDelegate(instance, channel: channel)
        } else if #available(iOS 9.0, *) {
            let instance = SignInWithAppleSafariPlugin()
            registrar.addMethodCallDelegate(instance, channel: channel)
            registrar.addApplicationDelegate(instance)     
        } else {
            let instance = SignInWithAppleUnavailablePlugin()
            registrar.addMethodCallDelegate(instance, channel: channel)
        }
    }
}
