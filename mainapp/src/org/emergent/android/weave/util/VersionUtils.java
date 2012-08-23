package org.emergent.android.weave.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * @author Peli
 * @version 2009-01-15
 */
public class VersionUtils {

  private static final String TAG = "VersionUtils";

  /**
   * Get current version number.
   *
   * @return
   */
  public static String getVersionNumber(Context context) {
    String version = "?";
    try {
      PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      version = pi.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Package name not found", e);
    }
    ;
    return version;
  }

  /**
   * Get application name.
   *
   * @return
   */
  public static String getApplicationName(Context context) {
    String name = "?";
    try {
      PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      name = context.getString(pi.applicationInfo.labelRes);
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Package name not found", e);
    }
    ;
    return name;
  }
}
