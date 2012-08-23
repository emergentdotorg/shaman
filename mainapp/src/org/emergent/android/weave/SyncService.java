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

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import org.emergent.android.weave.util.Dbg.Log;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.client.WeaveException;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.syncadapter.SyncAssistant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Patrick Woodworth
 */
public class SyncService extends IntentService implements Constants.Implementable {

  public static final String INTENT_EXTRA_OP_KEY = "op";
  public static final int INTENT_EXTRA_SYNC_REQUEST = 1001;
  public static final int INTENT_EXTRA_RESET_REQUEST = 1002;

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

    int opcode = extras.getInt(INTENT_EXTRA_OP_KEY, -1);
    switch (opcode) {
      case INTENT_EXTRA_RESET_REQUEST:
        onHandleResetIntent(intent);
        break;
      case INTENT_EXTRA_SYNC_REQUEST:
        onHandleSyncIntent(intent);
        break;
    }
  }

  private void onHandleSyncIntent(Intent intent) {
    Uri data = intent.getData();

    Messenger messenger = null;
    Bundle extras = intent.getExtras();
    if (extras != null) {
      messenger = (Messenger)extras.get(Constants.MESSENGER);
    }

    sendMessage(messenger, SyncEventType.STARTED);

//    Intent loginInfoIntent = intent.getParcelableExtra(Constants.LOGININFO);
//    WeaveAccountInfo loginInfo = SyncManager.toLoginInfo(loginInfoIntent);
    WeaveAccountInfo loginInfo = StaticUtils.getLoginInfo(this);
    boolean success = false;
    try {
      success = weaveUpdateSync(this, loginInfo);
      if (success) {
        sendMessage(messenger, SyncEventType.COMPLETED);
      } else {
        sendMessage(messenger, SyncEventType.FAILED);
      }
    } catch (Exception e) {
      Log.w(TAG, e);
      sendMessage(messenger, e);
    }
  }

  private void onHandleResetIntent(Intent intent) {
    try {
      wipeDataImpl2(this);
      Log.w(TAG, "resetCompleted!");
    } catch (Exception e) {
      Log.w(TAG, e);
    }
  }


  public static void wipeDataImpl2(Context context) {
    Log.w(TAG, "SyncService.wipeDataImpl2");
    try {
      SyncAssistant.resetCaches();
      ContentResolver resolver = context.getContentResolver();
      Passwords.UPDATER.deleteRecords(resolver);
      Bookmarks.UPDATER.deleteRecords(resolver);
    } catch (Throwable e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  static boolean weaveUpdateSync(Context context, WeaveAccountInfo loginInfo) throws Exception {
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

  private SyncEventType getSyncStatusType(Throwable e) {
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
