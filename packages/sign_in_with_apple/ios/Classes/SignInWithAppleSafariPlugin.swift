import AuthenticationServices
import SafariServices
import Flutter

@available(iOS 9.0, *)
public class SignInWithAppleSafariPlugin: NSObject, FlutterPlugin {
    fileprivate var _lastAuthorizationRequestResult: FlutterResult?
    fileprivate var _safariController: SFSafariViewController?
    fileprivate var _safariDelegate: SFSafariViewControllerDelegate?

    // This plugin should not be registered with directly
    //
    // This is merely a cross-platform plugin to handle the case Sign in with Apple is available
    // on the target platform
    //
    // Each target platform will still need a specific Plugin implementation
    // which will need to decide whether or not Sign in with Apple is available
    public static func register(with registrar: FlutterPluginRegistrar) {
        print("SignInWithAppleSafariPlugin tried to register which is not allowed")
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "isAvailable":
            result(true)

        case "isNativeSignInAvailable":
            result(false)
            
        case "performAuthorizationRequest":

            // Makes sure arguments exists and is a List
            guard let args = call.arguments as? [String:Any] else {
                result(
                    SignInWithAppleError.missingArguments(call).toFlutterError()
                )
                return
            }
        
            guard let url = args["url"] as? String else {
                result(
                    SignInWithAppleError.missingArgument(
                        call,
                        "url"
                    ).toFlutterError()
                )
                return
            }

            _lastAuthorizationRequestResult = result
            _safariController = SFSafariViewController(url: NSURL(string: url)! as URL)
            _safariDelegate = SignInWithAppleSafariDelegate(result)
            _safariController?.delegate = _safariDelegate

            let rootViewController = UIApplication.shared.delegate?.window??.rootViewController
            rootViewController?.present(_safariController!, animated: true, completion: nil)

        default:
            result(FlutterMethodNotImplemented)
        }
    }

    public func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {        
        if let result = _lastAuthorizationRequestResult {
            // let urlString = url.absoluteString
            // if (urlString == "user_cancelled_authorize") {
            //     result(
            //         FlutterError(
            //             code: "safari/canceled",
            //             message: "Canceled by the user",
            //             details: nil
            //         )
            //     )
            // }
            // else {
                result(url.absoluteString)
            // }
            _lastAuthorizationRequestResult = nil
        }
        if let safariController = _safariController {
            safariController.dismiss(animated: true, completion: nil)
            _safariController = nil
        }
        _safariDelegate = nil
        return true  
    }
}

@available(iOS 9.0, *)
fileprivate class SignInWithAppleSafariDelegate: NSObject, SFSafariViewControllerDelegate {
    var result: FlutterResult
    
    init(_ result: @escaping FlutterResult) {
        self.result = result
    }

    func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
        result(
            FlutterError(
                code: "safari/canceled",
                message: "Canceled by the user",
                details: nil
            )
        )
    }
}
