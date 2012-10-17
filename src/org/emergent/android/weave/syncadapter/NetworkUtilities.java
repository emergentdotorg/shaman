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

package org.emergent.android.weave.syncadapter;

import android.content.Context;
import android.content.SharedPreferences;

import org.emergent.android.weave.ShamanApplication;
import org.emergent.android.weave.StaticUtils;
import org.emergent.android.weave.client.UserWeave;
import org.emergent.android.weave.client.WeaveConstants;
import org.emergent.android.weave.PrefKey;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.client.WeaveException;
import org.emergent.android.weave.client.WeaveFactory;
import org.json.JSONException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Patrick Woodworth
 */
class NetworkUtilities {

  private static Map<Boolean, WeaveFactory> sm_factories = new ConcurrentHashMap<Boolean, WeaveFactory>();

  public static WeaveFactory getWeaveFactory(boolean allowInvalidCerts) {
    WeaveFactory retval = sm_factories.get(allowInvalidCerts);
    if (retval == null) {
      retval = new WeaveFactory(allowInvalidCerts);
      sm_factories.put(allowInvalidCerts, retval);
    }
    return retval;
  }

  public static UserWeave createUserWeave(WeaveAccountInfo accountInfo) {
    WeaveFactory factory = getWeaveFactory(allowAllCerts(accountInfo));
    return factory.createUserWeave(accountInfo.getServer(), accountInfo.getUsername(), accountInfo.getPassword());
  }

  public static String authenticate(String serverUri, String username, String password, String secret)
      throws WeaveException, JSONException, URISyntaxException
  {
    WeaveAccountInfo loginInfo = WeaveAccountInfo.createWeaveAccountInfo(serverUri, username, password, secret == null ? null : secret.toCharArray());
    NetworkUtilities.authenticate(loginInfo);
    return createAuthToken(serverUri, username, password, secret);
  }

  public static void authenticate(WeaveAccountInfo loginInfo) throws WeaveException, JSONException {
    UserWeave keyMgr = createUserWeave(loginInfo);
    keyMgr.authenticateSecret(loginInfo.getSecret());
  }

  public static String createAuthToken(String serverUri, String username, String password, String secret) {
    return WeaveAccountInfo.createWeaveAccountInfo(URI.create(serverUri), username, password, secret.toCharArray()).toAuthToken();
  }

  public static WeaveAccountInfo createWeaveAccountInfo(String authToken) throws URISyntaxException {
    return WeaveAccountInfo.createWeaveAccountInfo(authToken);
  }

  private static boolean allowAllCerts(WeaveAccountInfo info) {
    Context context = ShamanApplication.getInstance();
    SharedPreferences prefs = StaticUtils.getAccountPreferences(context, info);
    return prefs.getBoolean(PrefKey.allcerts.name(), WeaveConstants.ALLOW_INVALID_CERTS_DEFAULT);
  }
}
