/*
 * Copyright 2010 Patrick Woodworth
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.syncadapter.SyncAssistant;
import org.emergent.android.weave.util.Dbg;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Patrick Woodworth
 */
public class DobbyUtil {

  private static final String TAG = Dbg.getTag(DobbyUtil.class);

  private DobbyUtil() {
    // no instantiation
  }

  public static SharedPreferences getAccountPreferences(Context context, WeaveAccountInfo account) {
    return getSharedPreferences(context, null);
  }

  public static SharedPreferences getAccountPreferences(Context context, Account account) {
    return getSharedPreferences(context, null);
  }

  public static SharedPreferences getApplicationPreferences(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static SharedPreferences getSharedPreferences(Context context, WeaveAccountInfo account) {
//    return context.getSharedPreferences("", Context.MODE_PRIVATE);
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static SharedPreferences.Editor intentToLoginPrefs(SharedPreferences.Editor editor, Intent data) {
    if (data != null) {
      editor.putString(PrefKey.server_url.name(), data.getStringExtra(Constants.USERDATA_SERVER_KEY));
      editor.putString(PrefKey.authAccount.name(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
      editor.putString(PrefKey.password.name(), data.getStringExtra(AccountManager.KEY_PASSWORD));
      editor.putString(PrefKey.sync_key.name(), data.getStringExtra(Constants.USERDATA_SECRET_KEY));
    }
    return editor;
  }

  public static Intent loginPrefsToIntent(SharedPreferences loginInfo, Intent intent) {
    if (loginInfo != null) {
      intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, loginInfo.getString(PrefKey.authAccount.name(), null));
      intent.putExtra(AccountManager.KEY_PASSWORD, loginInfo.getString(PrefKey.password.name(), null));
      intent.putExtra(Constants.USERDATA_SERVER_KEY, loginInfo.getString(PrefKey.server_url.name(), null));
      intent.putExtra(Constants.USERDATA_SECRET_KEY, loginInfo.getString(PrefKey.sync_key.name(), null));
    }
    return intent;
  }

  public static Intent loginInfoToIntent(WeaveAccountInfo loginInfo, Intent intent) {
    if (loginInfo != null) {
      intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, loginInfo.getUsername());
      intent.putExtra(AccountManager.KEY_PASSWORD, loginInfo.getPassword());
      intent.putExtra(Constants.USERDATA_SERVER_KEY, loginInfo.getServerAsString());
      intent.putExtra(Constants.USERDATA_SECRET_KEY, loginInfo.getSecretAsString());
    }
    return intent;
  }

  public static WeaveAccountInfo intentToLogin(Intent data) {
    return WeaveAccountInfo.createWeaveAccountInfo(
        URI.create(data.getStringExtra(Constants.USERDATA_SERVER_KEY)),
        data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),
        data.getStringExtra(AccountManager.KEY_PASSWORD),
        data.getStringExtra(Constants.USERDATA_SECRET_KEY).toCharArray()
    );
  }

  public static boolean checkUpgrade(Context context) {
    SharedPreferences prefs = getApplicationPreferences(context);
    dumpPrefs(prefs);

    int currentVersionCode = getApplicationVersionCode(context);
    int firstVersion = PrefKey.firstVersionCode.getInt(prefs, 0);
    int lastVersion = PrefKey.lastVersionCode.getInt(prefs, 0);
    if (lastVersion == currentVersionCode) {
      return false;
    }

    wipeDataImpl(context);
    SharedPreferences.Editor e = prefs.edit();
    if (firstVersion < 1) {
      e.putInt(PrefKey.firstVersionCode.name(), currentVersionCode);
    }
    e.putInt(PrefKey.lastVersionCode.name(), currentVersionCode);
    return (e.commit());

  }

  public static void wipeDataImpl(Context context) {
    Log.w(TAG, "wipeData");
    ContentResolver resolver = context.getContentResolver();
    Passwords.UPDATER.deleteRecords(resolver);
    Bookmarks.UPDATER.deleteRecords(resolver);
    try {
      final Set<SyncAssistant> syncAssistants = new HashSet<SyncAssistant>(Arrays.asList(
          new SyncAssistant(context, Bookmarks.UPDATER),
          new SyncAssistant(context, Passwords.UPDATER)
      ));

      for (SyncAssistant syncAssistant : syncAssistants) {
        syncAssistant.reset();
      }
    } catch (Throwable e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  public static void dumpPrefs(SharedPreferences prefs) {
    try {
      Map<String, ?> allPrefs = prefs.getAll();
      Set<? extends Map.Entry<String, ?>> entrySet = allPrefs.entrySet();
      Log.d(TAG, String.format("prefs: count='%d'", entrySet.size()));
      for (Map.Entry<String, ?> entry : entrySet) {
        String key = entry.getKey();
        Object val = null;
        String type = "?";
        try {
          val = entry.getValue();
          type = val == null ? "null" : val.getClass().getName();
        } catch (Throwable ignored) {
        }
        if (PrefKey.password.name().equals(key) || PrefKey.sync_key.name().equals(key)) {
          val = "******";
        }
        Log.d(TAG, String.format("  pref: key='%s' ; type='%s' ; val='%s'", key, type, val));
      }
    } catch (Throwable e) {
      Log.e(TAG, e.getLocalizedMessage(), e);
    }
  }

  public static int getApplicationVersionCode(Context context) {
    int retval = 0;
    try {
      String pkgName = context.getPackageName();
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
      retval = pInfo.versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, e.getLocalizedMessage(), e);
    }
    return retval;
  }

  public static String getApplicationVersioName(Context context) {
    String retval = null;
    try {
      String pkgName = context.getPackageName();
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
      retval = pInfo.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, e.getLocalizedMessage(), e);
    }
    return retval;
  }
}
