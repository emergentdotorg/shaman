package org.emergent.android.weave.util;

import android.util.Log;

/**
 * @author Patrick Woodworth
 */
public class Dbg {

  static final String LOG_TAG = "EmergentWeave";

  public static final String TAG = LOG_TAG;

  @SuppressWarnings("ConstantConditions")
  public static class Log {

    private static final int SHAMAN_LOG_LEVEL = android.util.Log.VERBOSE;

    public static void v(String tag, String msg) {
      if (android.util.Log.VERBOSE >= SHAMAN_LOG_LEVEL) android.util.Log.v(tag, msg);
    }

    public static void d(String tag, String msg) {
      if (android.util.Log.DEBUG >= SHAMAN_LOG_LEVEL) android.util.Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
      if (android.util.Log.DEBUG >= SHAMAN_LOG_LEVEL) android.util.Log.d(tag, msg, tr);
    }

    public static void i(String tag, String msg) {
      if (android.util.Log.INFO >= SHAMAN_LOG_LEVEL) android.util.Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
      if (android.util.Log.WARN >= SHAMAN_LOG_LEVEL) android.util.Log.w(tag, msg);
    }

    public static void w(String tag, Throwable tr) {
      if (android.util.Log.WARN >= SHAMAN_LOG_LEVEL) android.util.Log.w(tag, tr);
    }

    public static void w(String tag, String msg, Throwable tr) {
      if (android.util.Log.WARN >= SHAMAN_LOG_LEVEL) android.util.Log.w(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
      if (android.util.Log.ERROR >= SHAMAN_LOG_LEVEL) android.util.Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
      if (android.util.Log.ERROR >= SHAMAN_LOG_LEVEL) android.util.Log.e(tag, msg, tr);
    }
  }

//  public static String getTag(Class clazz) {
//    return LOG_TAG;
//  }
//
//  public static void logw(String msg) {
//    Log.w(LOG_TAG, msg);
//  }
//
//  public static void logw(String pattern, Object... args) {
//    String msg = String.format(pattern, args);
//    Log.w(LOG_TAG, msg);
//  }
//
//  public static void trace(Object o, Class aClass, String methodName) {
//    trace(o, aClass, methodName, null);
//  }
//
//  public static void trace(Object o, Class aClass, String methodName, String s) {
//    String cName = (o != null) ? o.getClass().getSimpleName() : aClass.getSimpleName();
//    if (s == null)
//      s = "";
//    String msg = String.format("trace: (%s.%s)%s", cName, methodName, s);
//    Log.w(TAG, msg);
//  }
//
//  private static void println(String msg) {
//    Log.i(LOG_TAG, msg);
//  }
//
//  private static void printf(String pattern, Object... args) {
//    String msg = String.format(pattern, args);
//    Log.i(LOG_TAG, msg);
//  }
//
//  private static void error(Throwable e) {
//    Log.e(LOG_TAG, e.getMessage(), e);
//  }
//
//  private static void error(String message, Throwable e) {
//    Log.e(LOG_TAG, message, e);
//  }
}
