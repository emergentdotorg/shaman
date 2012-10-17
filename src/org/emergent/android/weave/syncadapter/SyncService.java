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

import org.emergent.android.weave.ApiCompatUtil;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.StaticUtils;
import org.emergent.android.weave.SyncEventType;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.client.WeaveException;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.util.Dbg.*;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Patrick Woodworth
 */
public class SyncService extends IntentService implements Constants.Implementable {

  static final int INTENT_EXTRA_SYNC_REQUEST = 1001;
  static final int INTENT_EXTRA_RESET_REQUEST = 1002;
  static final int INTENT_EXTRA_AUTH_REQUEST = 1003;

  private long m_lastSyncRequestTime = 0;

  public SyncService() {
    super("WeaveSyncService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Bundle extras = intent.getExtras();
    if (extras == null) {
      Log.e(TAG, "No op specified!");
      return;
    }

    int opcode = extras.getInt(SyncUtil.INTENT_EXTRA_SYNC_OP_KEY, -1);
    switch (opcode) {
      case INTENT_EXTRA_RESET_REQUEST:
        onHandleResetIntent(intent);
        break;
      case INTENT_EXTRA_SYNC_REQUEST:
        onHandleSyncIntent(intent);
        break;
      case INTENT_EXTRA_AUTH_REQUEST:
        onHandleAuthIntent(intent);
        break;
    }
  }

  private void onHandleAuthIntent(Intent intent) {
    Messenger messenger = null;
    WeaveAccountInfo loginInfo = null;
    Bundle loginBundle = null;
    Bundle extras = intent.getExtras();
    try {
      loginBundle = extras.getBundle(SyncUtil.INTENT_EXTRA_SYNC_LOGININFO);
      messenger = (Messenger)extras.get(SyncUtil.INTENT_EXTRA_SYNC_MESSENGER_KEY);
      loginInfo = StaticUtils.bundleToLogin(loginBundle);
      NetworkUtilities.authenticate(loginInfo);
      sendAuthMessage(messenger, loginBundle);
    } catch (Exception e) {
      Log.w(TAG, e);
      sendAuthMessage(messenger, loginBundle, e);
    }
  }

  private void onHandleSyncIntent(Intent intent) {
//    Uri data = intent.getData();
    Bundle extras = intent.getExtras();
    long reqTimeStamp = extras.getLong(SyncUtil.INTENT_EXTRA_SYNC_REQTIME);
    if (reqTimeStamp < m_lastSyncRequestTime) {
      Log.w(TAG, "Skipping redundant sync!");
      return;
    }
    Messenger messenger = (Messenger)extras.get(SyncUtil.INTENT_EXTRA_SYNC_MESSENGER_KEY);
    Bundle loginBundle = extras.getBundle(SyncUtil.INTENT_EXTRA_SYNC_LOGININFO);
    WeaveAccountInfo loginInfo = StaticUtils.bundleToLogin(loginBundle);
    boolean success = false;
    ApiCompatUtil apiCompatUtil = ApiCompatUtil.getInstance();
    try {
      apiCompatUtil.postSyncNotification(this);
      success = weaveUpdateSync(this, loginInfo);
      if (success) {
        sendMessage(messenger, SyncEventType.COMPLETED);
      } else {
        sendMessage(messenger, SyncEventType.FAILED);
      }
    } catch (Exception e) {
      Log.w(TAG, e);
      sendMessage(messenger, e);
    } finally {
      m_lastSyncRequestTime = System.currentTimeMillis();
      apiCompatUtil.clearSyncNotification(this);
    }
  }

  private void onHandleResetIntent(Intent intent) {
    try {
      m_lastSyncRequestTime = 0;
      wipeDataImpl2(this);
      Log.w(TAG, "resetCompleted!");
    } catch (Exception e) {
      Log.w(TAG, e);
    }
  }

  private static void wipeDataImpl2(Context context) {
    try {
      SyncAssistant.resetCaches();
      ContentResolver resolver = context.getContentResolver();
      Passwords.UPDATER.deleteRecords(resolver);
      Bookmarks.UPDATER.deleteRecords(resolver);
    } catch (Throwable e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  private static boolean weaveUpdateSync(Context context, WeaveAccountInfo loginInfo) throws Exception {
    if (loginInfo == null)
      return false;

    final Set<SyncAssistant> syncAssistants = new HashSet<SyncAssistant>(Arrays.asList(
        new SyncAssistant(context, Bookmarks.UPDATER),
        new SyncAssistant(context, Passwords.UPDATER)
    ));

    for (SyncAssistant syncAssistant : syncAssistants) {
      syncAssistant.doQueryAndUpdate(loginInfo.toAuthToken());
    }

    return true;
  }

  private void sendMessage(Messenger messenger, Throwable e) {
    SyncEventType type = getSyncStatusType(e);
    String text = String.format("%s : %s", e.getClass().getSimpleName(), e.getLocalizedMessage());
    sendMessage(messenger, type, text);
  }

  private void sendMessage(Messenger messenger, SyncEventType type) {
    sendMessage(messenger, type, null);
  }

  private void sendMessage(Messenger messenger, SyncEventType type, String text) {
    if (messenger == null)
      return;
    Message msg = Message.obtain();
    msg.arg1 = Constants.SYNC_EVENT;
    msg.arg2 = type.ordinal();
    msg.obj = text;
    try {
      messenger.send(msg);
    } catch (android.os.RemoteException e1) {
      Log.w(TAG, "Exception sending message", e1);
    }
  }

  private void sendAuthMessage(Messenger messenger, Bundle loginBundle) {
    sendAuthMessage(messenger, loginBundle, null);
  }

  private void sendAuthMessage(Messenger messenger, Bundle loginBundle, Throwable e) {
    SyncEventType type = (e == null) ? SyncEventType.COMPLETED : SyncEventType.FAILED;
    Message msg = Message.obtain();
    msg.arg1 = Constants.SYNC_EVENT;
    msg.arg2 = type.ordinal();
    msg.obj = loginBundle;
    try {
      messenger.send(msg);
    } catch (android.os.RemoteException e1) {
      Log.w(TAG, "Exception sending message", e1);
    }
  }


  private static SyncEventType getSyncStatusType(Throwable e) {
    if (e != null) {
      if (e instanceof WeaveException) {
        switch (((WeaveException)e).getType()) {
          case UNAUTHORIZED:
            return SyncEventType.BAD_PASSWORD;
          case NOTFOUND:
            return SyncEventType.BAD_USERNAME;
          case CRYPTO:
            return SyncEventType.BAD_SYNCKEY;
          default:
            break;
        }
      }
    }
    return SyncEventType.FAILED;
  }
}
