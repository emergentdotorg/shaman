/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.emergent.android.weave;

import android.accounts.AccountManager;
import org.emergent.android.weave.util.Dbg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class Constants {

  public static final String APP_PACKAGE_NAME = Constants.class.getPackage().getName();

  public static final String BOOKMARK_PROVIDER_AUTHORITY = "org.emergent.android.weave.bookmarkcontentprovider";

  public static final String PASSWORD_PROVIDER_AUTHORITY = "org.emergent.android.weave.passwordcontentprovider";

  public static final int EDIT_ACCOUNT_LOGIN_REQUEST_CODE = 1000;

  /**
   * Account type string.
   */
  public static final String ACCOUNT_TYPE = "org.emergent.android.weave";

  /**
   * Authtoken type string.
   */
  public static final String AUTHTOKEN_TYPE = "org.emergent.android.weave";

  public static final String USERDATA_USERNAME_KEY = AccountManager.KEY_ACCOUNT_NAME;

  public static final String USERDATA_PASSWORD_KEY = AccountManager.KEY_PASSWORD;

  public static final String USERDATA_SERVER_KEY = "server_url";

  public static final String USERDATA_SECRET_KEY = "sync_key";

  public static final String ROW_ID_INTENT_EXTRA_KEY = APP_PACKAGE_NAME + ".rowId";

  public static final int SYNC_EVENT = 1;

  static final boolean MENUITEM_HELP_DISABLED = true;
  static final boolean MENUITEM_RESET_DISABLED = false;
  static final boolean MENUITEM_SETTINGS_DISABLED = true;

  private static final String RUNTIME_PROPERTIES_PATH = "/sdcard/weave.properties";

  private static final File RUNTIME_PROPS_FILE = new File(RUNTIME_PROPERTIES_PATH);

  private static final Properties sm_runtimeProps = new Properties();

  private static final AtomicLong sm_runtimePropsLastReload = new AtomicLong(0);
  public static final boolean MENUITEM_HOME_DISABLED = ApiCompatUtil.IS_HONEYCOMB;


  public static Properties getRuntimeDefaults() {
    long lastReload = sm_runtimePropsLastReload.get();
    if (RUNTIME_PROPS_FILE.isFile()) {
      long lastFileMod = RUNTIME_PROPS_FILE.lastModified();
      if (lastFileMod > lastReload) {
        if (sm_runtimePropsLastReload.compareAndSet(lastReload, lastFileMod)) {
          InputStream is = null;
          try {
            is = new BufferedInputStream(new FileInputStream(RUNTIME_PROPS_FILE));
            sm_runtimeProps.clear();
            sm_runtimeProps.load(is);
          } catch (Exception ignored) {
          } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) { }
          }
        }
      }
    }
    return sm_runtimeProps;
  }

  public static interface Implementable {

    public static final String TAG = Dbg.TAG;
    String FRAG_TAG_BUNDLE_KEY = "fragTag";
  }
}
