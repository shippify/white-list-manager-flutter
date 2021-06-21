package com.shippify.co.white_list_manager;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** WhiteListManagerPlugin */
public class WhiteListManagerPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Context context;

  public enum WhiteListedInBatteryOptimizations {
    WHITE_LISTED, NOT_WHITE_LISTED, ERROR_GETTING_STATE, UNKNOWN_TOO_OLD_ANDROID_API_FOR_CHECKING, IRRELEVANT_OLD_ANDROID_API
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    context = flutterPluginBinding.getApplicationContext();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "white_list_manager");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("addToWhiteList")) {
      String packageName = call.argument("package");
      Intent powerInt = prepareIntentForWhiteListingOfBatteryOptimization(context, packageName != null ? packageName : context.getPackageName(),false);
      if (powerInt != null) {
        context.startActivity(powerInt);
      }
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @NonNull
  public static WhiteListedInBatteryOptimizations getIfAppIsWhiteListedFromBatteryOptimizations(@NonNull Context context, @NonNull String packageName) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
      return WhiteListedInBatteryOptimizations.IRRELEVANT_OLD_ANDROID_API;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
      return WhiteListedInBatteryOptimizations.UNKNOWN_TOO_OLD_ANDROID_API_FOR_CHECKING;
    final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    if (pm == null)
      return WhiteListedInBatteryOptimizations.ERROR_GETTING_STATE;
    return pm.isIgnoringBatteryOptimizations(packageName) ? WhiteListedInBatteryOptimizations.WHITE_LISTED : WhiteListedInBatteryOptimizations.NOT_WHITE_LISTED;
  }

  @TargetApi(Build.VERSION_CODES.M)
  @RequiresPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
  @Nullable
  public static Intent prepareIntentForWhiteListingOfBatteryOptimization(@NonNull Context context, @NonNull String packageName, boolean alsoWhenWhiteListed) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
      return null;
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_DENIED) {
      return null;
    }
    final WhiteListedInBatteryOptimizations appIsWhiteListedFromPowerSave = getIfAppIsWhiteListedFromBatteryOptimizations(context, packageName);
    Intent intent = null;
    switch (appIsWhiteListedFromPowerSave) {
      case WHITE_LISTED:
        if (alsoWhenWhiteListed)
          intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        break;
      case NOT_WHITE_LISTED:
        intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + packageName));
        break;
      case ERROR_GETTING_STATE:
      case UNKNOWN_TOO_OLD_ANDROID_API_FOR_CHECKING:
      case IRRELEVANT_OLD_ANDROID_API:
      default:
        break;
    }
    return intent;
  }
}
