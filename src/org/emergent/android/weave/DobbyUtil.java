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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.emergent.android.weave.client.WeaveAccountInfo;

import java.net.URI;

/**
 * @author Patrick Woodworth
 */
public class DobbyUtil {

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
      editor.putString(PrefKey.username.name(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
      editor.putString(PrefKey.password.name(), data.getStringExtra(AccountManager.KEY_PASSWORD));
      editor.putString(PrefKey.sync_key.name(), data.getStringExtra(Constants.USERDATA_SECRET_KEY));
    }
    return editor;
  }

  public static Intent loginPrefsToIntent(SharedPreferences loginInfo, Intent intent) {
    if (loginInfo != null) {
      intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, loginInfo.getString(PrefKey.username.name(), null));
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
}
