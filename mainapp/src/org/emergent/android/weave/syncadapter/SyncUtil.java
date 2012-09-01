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

package org.emergent.android.weave.syncadapter;

import org.emergent.android.weave.Constants;
import org.emergent.android.weave.StaticUtils;
import org.emergent.android.weave.client.WeaveAccountInfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;

import java.nio.BufferUnderflowException;

/**
 * @author Patrick Woodworth
 */
public class SyncUtil {

  public static final String INTENT_EXTRA_SYNC_MESSENGER_KEY = Constants.APP_PACKAGE_NAME + ".Messenger";
  public static final String INTENT_EXTRA_SYNC_LOGININFO = Constants.APP_PACKAGE_NAME + ".LoginInfo";
  public static final String INTENT_EXTRA_SYNC_REQTIME = Constants.APP_PACKAGE_NAME + ".RequestTimestamp";

  static final String INTENT_EXTRA_SYNC_OP_KEY = Constants.APP_PACKAGE_NAME + ".SyncOp";

  private SyncUtil() {
  }

  public static void requestAuth(Activity activity, WeaveAccountInfo loginInfo, Handler handler) {
    if (loginInfo == null)
      return;

    Intent intent = new Intent(activity, SyncService.class);
    intent.putExtra(INTENT_EXTRA_SYNC_OP_KEY, SyncService.INTENT_EXTRA_AUTH_REQUEST);
    intent.putExtra(INTENT_EXTRA_SYNC_REQTIME, System.currentTimeMillis());
    Bundle loginBundle = StaticUtils.loginToBundle(loginInfo);
    intent.putExtra(INTENT_EXTRA_SYNC_LOGININFO, loginBundle);
    if (handler != null) {
      // Create a new Messenger for the communication back
      Messenger messenger = new Messenger(handler);
      intent.putExtra(INTENT_EXTRA_SYNC_MESSENGER_KEY, messenger);
    }
    activity.startService(intent);
  }

  public static void requestSync(Activity activity, WeaveAccountInfo loginInfo, Handler handler) {
    if (loginInfo == null)
      return;

    Intent intent = new Intent(activity, SyncService.class);
    intent.putExtra(INTENT_EXTRA_SYNC_OP_KEY, SyncService.INTENT_EXTRA_SYNC_REQUEST);
    intent.putExtra(INTENT_EXTRA_SYNC_REQTIME, System.currentTimeMillis());
    Bundle loginBundle = StaticUtils.loginToBundle(loginInfo);
    intent.putExtra(INTENT_EXTRA_SYNC_LOGININFO, loginBundle);
    if (handler != null) {
      // Create a new Messenger for the communication back
      Messenger messenger = new Messenger(handler);
      intent.putExtra(INTENT_EXTRA_SYNC_MESSENGER_KEY, messenger);
    }
    activity.startService(intent);
  }

  public static void requestReset(Context activity, Handler handler) {
    Intent intent = new Intent(activity, SyncService.class);
    intent.putExtra(INTENT_EXTRA_SYNC_OP_KEY, SyncService.INTENT_EXTRA_RESET_REQUEST);
    intent.putExtra(INTENT_EXTRA_SYNC_REQTIME, System.currentTimeMillis());
    if (handler != null) {
      // Create a new Messenger for the communication back
      Messenger messenger = new Messenger(handler);
      intent.putExtra(INTENT_EXTRA_SYNC_MESSENGER_KEY, messenger);
    }
    activity.startService(intent);
  }
}
