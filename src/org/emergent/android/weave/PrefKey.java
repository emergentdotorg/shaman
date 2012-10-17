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

import android.content.SharedPreferences;
import android.content.res.Resources;

/**
 * @author Patrick Woodworth
 */
public enum PrefKey {
  allcerts(R.bool.pref_accept_invalid_certs_default), // boolean
  opentab(R.integer.pref_opentab_default), // int
  sync_period, // unused
  sync_on_open(R.bool.pref_sync_on_open_default), // boolean
  server_url(R.string.pref_server_url_default), // String
  authAccount, // String
  password, // String
  sync_key, // String
  firstVersionCode, // int
  lastVersionCode, // int
  lastSync, // long
  lastPrefSave, // long
  ;

  private final int m_prefDefaultId;

  private PrefKey() {
    this(-1);
  }

  private PrefKey(int prefDefaultId) {
    m_prefDefaultId = prefDefaultId;
  }

  public boolean getBoolean(SharedPreferences preferences, Resources resources) {
    boolean def = m_prefDefaultId != -1 && resources.getBoolean(m_prefDefaultId);
    return preferences.getBoolean(name(), def);
  }

  public int getInt(SharedPreferences preferences, int def) {
    return preferences.getInt(name(), def);
  }

  public int getInt(SharedPreferences preferences, Resources resources) {
    int def = m_prefDefaultId == -1 ? 0 : resources.getInteger(m_prefDefaultId);
    return preferences.getInt(name(), def);
  }
}
