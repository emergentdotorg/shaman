/*
 * Copyright 2012 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.emergent.android.weave;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import org.emergent.android.weave.util.Dbg.Log;

import java.io.File;

/**
 * @author Patrick Woodworth
 */
public class ShamanApplication extends Application implements Constants.Implementable {

  private static ShamanApplication sm_instance;

  @Override
  public void onCreate() {
    super.onCreate();
    sm_instance = this;
  }

  public static ShamanApplication getInstance() {
    return sm_instance;
  }

  public static String getApplicationName() {
    String retval = "?";
    try {
      Context context = getInstance();
      String pkgName = context.getPackageName();
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
      retval = context.getString(pInfo.applicationInfo.labelRes);
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(StaticUtils.TAG, "Application name not found!", e);
    }
    return retval;
  }

  public static int getApplicationVersionCode() {
    int retval = 0;
    try {
      Context context = getInstance();
      String pkgName = context.getPackageName();
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
      retval = pInfo.versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(StaticUtils.TAG, "Application versionCode not found!", e);
    }
    return retval;
  }

  public static String getApplicationVersionName() {
    String retval = null;
    try {
      Context context = getInstance();
      String pkgName = context.getPackageName();
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
      retval = pInfo.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(StaticUtils.TAG, "Application versionName not found!", e);
    }
    return retval;
  }

  private void onDestroy() {
    // closing Entire Application
    android.os.Process.killProcess(android.os.Process.myPid());
    SharedPreferences.Editor editor = getSharedPreferences("clear_cache", Context.MODE_PRIVATE).edit();
    editor.clear();
    editor.commit();
    trimCache(this);
  }


  private static void trimCache(Context context) {
    try {
      File dir = context.getCacheDir();
      if (dir != null && dir.isDirectory()) {
        deleteDir(dir);

      }
    } catch (Exception e) {
      Log.e(TAG, "Error cleaning cache dir!", e);
    }
  }


  private void clearApplicationData() {
    File cache = getCacheDir();
    File appDir = new File(cache.getParent());
    if (appDir.exists()) {
      String[] children = appDir.list();
      for (String s : children) {
        if (!s.equals("lib")) {
          deleteDir(new File(appDir, s));
          Log.i("TAG", "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
        }
      }
    }
  }

  private static boolean deleteDir(File dir) {
    if (dir != null && dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }

    return dir.delete();
  }
}
