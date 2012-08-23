package org.emergent.android.weave.util;

import android.util.Log;

/**
 * @author Patrick Woodworth
 */
public class Dbg {

  public static final String LOG_TAG = "EmergentWeave";

  public static String getTag(Class clazz) {
    return LOG_TAG;
  }

  public static void println(String msg) {
    Log.i(LOG_TAG, msg);
  }

  public static void printf(String pattern, Object... args) {
    String msg = String.format(pattern, args);
    Log.i(LOG_TAG, msg);
  }

  public static void error(Throwable e) {
    Log.e(LOG_TAG, e.getMessage(), e);
  }

  public static void error(String message, Throwable e) {
    Log.e(LOG_TAG, message, e);
  }
}
