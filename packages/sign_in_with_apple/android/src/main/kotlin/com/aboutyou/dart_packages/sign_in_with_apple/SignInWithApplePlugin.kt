package com.aboutyou.dart_packages.sign_in_with_apple

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

import android.net.Uri
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent

import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

/** SignInWithApplePlugin */
public class SignInWithApplePlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private var channel: MethodChannel? = null

  var activity: Activity? = null
  

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.aboutyou.dart_packages.sign_in_with_apple")
    channel?.setMethodCallHandler(this);
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel?.setMethodCallHandler(null)
    channel = null
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    var lastAuthorizationRequestResult: Result? = null
    var triggerMainActivityToHideChromeCustomTab : (() -> Unit)? = null

    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "com.aboutyou.dart_packages.sign_in_with_apple")
      channel.setMethodCallHandler(SignInWithApplePlugin())
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "isAvailable" -> result.success(true)
      "isNativeSignInAvailable" -> result.success(false)
      "performAuthorizationRequest" -> {
        val _activity = activity

        if (_activity == null) {
          result.error("MISSING_ACTIVITY", "Plugin is not attached to an activity", call.arguments)
          return
        }

        val url: String? = call.argument("url")

        if (url == null) {
          result.error("MISSING_ARG", "Missing 'url' argument", call.arguments)
          return
        }

        SignInWithApplePlugin.lastAuthorizationRequestResult?.error("NEW_REQUEST", "A new request came in while this was still pending. The previous request (this one) was then cancelled.", null)
        if (SignInWithApplePlugin.triggerMainActivityToHideChromeCustomTab != null) {
          SignInWithApplePlugin.triggerMainActivityToHideChromeCustomTab!!()
        }

        SignInWithApplePlugin.lastAuthorizationRequestResult = result
        SignInWithApplePlugin.triggerMainActivityToHideChromeCustomTab = {
          val notificationIntent = _activity.getPackageManager().getLaunchIntentForPackage(_activity.getPackageName());
          notificationIntent.setPackage(null)
          notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          _activity.startActivity(notificationIntent)
        }

        val intent = ChromeCustomTabsCaller.newIntent(_activity, url);
        _activity.startActivity(intent)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onDetachedFromActivity() {
    activity = null
  }
}

/**
 * Activity which is used when the web-based authentication flow links back to the app
 *
 * DO NOT rename this or it's package name as it's configured in the consumer's `AndroidManifest.xml`
 *
 */
public class SignInWithAppleCallback: Activity {
  constructor() : super()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val lastAuthorizationRequestResult = SignInWithApplePlugin.lastAuthorizationRequestResult
    if (lastAuthorizationRequestResult != null) {
      lastAuthorizationRequestResult.success(intent?.data?.toString())
      SignInWithApplePlugin.lastAuthorizationRequestResult = null
    } else {
      SignInWithApplePlugin.triggerMainActivityToHideChromeCustomTab = null

      throw Exception("Received Sign in with Apple callback, but 'lastAuthorizationRequestResult' function was `null`")
    }

    val triggerMainActivityToHideChromeCustomTab = SignInWithApplePlugin.triggerMainActivityToHideChromeCustomTab
    if (triggerMainActivityToHideChromeCustomTab != null) {
      triggerMainActivityToHideChromeCustomTab()
      SignInWithApplePlugin.triggerMainActivityToHideChromeCustomTab = null
    } else {
      throw Exception("Received Sign in with Apple callback, but 'triggerMainActivityToHideChromeCustomTab' function was `null`")
    }

    finish()
  }
}

/**
 * Activity used to call the chrome custom tabs and get a result if it is canceled
 */
public class ChromeCustomTabsCaller: Activity {

  companion object {
    private val CUSTOM_TABS_REQUEST_CODE = 1

    fun newIntent(context: Context, url: String): Intent {
      val intent = Intent(context, ChromeCustomTabsCaller::class.java)
      intent.putExtra("url", url)
      return intent
    }
  } 

  constructor() : super()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val url = intent.getStringExtra("url")
      ?: throw IllegalStateException("Missing 'url' argument")

    val builder = CustomTabsIntent.Builder()
    val customTabsIntent = builder.build()
    customTabsIntent.intent.setData(Uri.parse(url))
    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

    startActivityForResult(customTabsIntent.intent, CUSTOM_TABS_REQUEST_CODE);
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == CUSTOM_TABS_REQUEST_CODE) {
      
      if (resultCode == Activity.RESULT_CANCELED) {
        val lastAuthorizationRequestResult = SignInWithApplePlugin.lastAuthorizationRequestResult
        if (lastAuthorizationRequestResult != null) {
          lastAuthorizationRequestResult.error("android/canceled", "Canceled by the user", null)
          SignInWithApplePlugin.lastAuthorizationRequestResult = null
        }
      }

      finish();
    }
    else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }
}
