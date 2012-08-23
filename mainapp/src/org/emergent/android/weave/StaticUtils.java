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
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Messenger;
import android.preference.PreferenceManager;
import org.emergent.android.weave.util.Dbg.Log;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.syncadapter.LoginActivity;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * @author Patrick Woodworth
 */
public class StaticUtils implements Constants.Implementable {

  private StaticUtils() {
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

    int currentVersionCode = ShamanApplication.getApplicationVersionCode();
    int firstVersion = PrefKey.firstVersionCode.getInt(prefs, 0);
    int lastVersion = PrefKey.lastVersionCode.getInt(prefs, 0);
    if (lastVersion == currentVersionCode) {
      return false;
    }

    Log.i(TAG, "Upgrade found!");
    wipeDataImpl(context);
    SharedPreferences.Editor e = prefs.edit();
    if (firstVersion < 1) {
      e.putInt(PrefKey.firstVersionCode.name(), currentVersionCode);
    }
    e.putInt(PrefKey.lastVersionCode.name(), currentVersionCode);
    boolean retval = (e.commit());
    dumpPrefs(prefs);
    return retval;
  }

  public static void wipeDataImpl(Context context) {
    Log.w(TAG, "wipeData");
    requestReset(context, null);
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

  static WeaveAccountInfo getLoginInfo(Context context) {
    WeaveAccountInfo loginInfo = null;
    try {
      Intent intent = getLoginInfoIntent(context);
      loginInfo = toLoginInfo(intent);
    } catch (Exception e) {
      Log.d(TAG, e.getMessage(), e);
    }
    return loginInfo;
  }

  private static Intent getLoginInfoIntent(Context context) {
    Intent intent = new Intent();
    loginPrefsToIntent(getApplicationPreferences(context), intent);
    return intent;
  }

  private static WeaveAccountInfo toLoginInfo(Intent intent) {
    WeaveAccountInfo loginInfo = null;
    try {
      loginInfo = intentToLogin(intent);
    } catch (Exception e) {
      Log.d(TAG, e.getMessage(), e);
    }
    return loginInfo;
  }

  public static boolean requestSync(Activity activity, Handler handler) {
    WeaveAccountInfo loginInfo = getLoginInfo(activity);
    if (loginInfo == null) {
      launchLoginEditor(activity);
    } else {
      requestSync(activity, loginInfo, handler);
    }
    return true;
  }

  public static void launchHelp(Activity activity) {
    Uri uriUrl = Uri.parse(activity.getResources().getString(R.string.help_url));
    Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
    activity.startActivity(intent);
  }

  public static void launchPreferencesEditor(Activity activity) {
    Intent intent = new Intent();
    intent.setClass(activity, ApplicationOptionsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activity.startActivity(intent);
  }

  public static void launchLoginEditor(Activity activity) {
    Intent intent = new Intent();
    intent.setClass(activity, LoginActivity.class);
    loginPrefsToIntent(getApplicationPreferences(activity), intent);
    activity.startActivityForResult(intent, Constants.EDIT_ACCOUNT_LOGIN_REQUEST_CODE);
  }

  public static void wipeData(final Activity activity) {
    AlertDialog adb = (new AlertDialog.Builder(activity))
        .setTitle(R.string.reset_confirm_title)
        .setMessage(R.string.reset_confirm_message)
        .setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            wipeDataImpl(activity);
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  public static void requestSync(Activity activity, WeaveAccountInfo loginInfo, Handler handler) {
    if (loginInfo == null)
      return;

    Intent intent = new Intent(activity, SyncService.class);
    intent.putExtra(SyncService.INTENT_EXTRA_OP_KEY, SyncService.INTENT_EXTRA_SYNC_REQUEST);
    if (handler != null) {
      // Create a new Messenger for the communication back
      Messenger messenger = new Messenger(handler);
      intent.putExtra(Constants.MESSENGER, messenger);
    }
    activity.startService(intent);
  }

  public static void requestReset(Context activity, Handler handler) {
    Intent intent = new Intent(activity, SyncService.class);
    intent.putExtra(SyncService.INTENT_EXTRA_OP_KEY, SyncService.INTENT_EXTRA_RESET_REQUEST);
    if (handler != null) {
      // Create a new Messenger for the communication back
      Messenger messenger = new Messenger(handler);
      intent.putExtra(Constants.MESSENGER, messenger);
    }
    activity.startService(intent);
  }

}
