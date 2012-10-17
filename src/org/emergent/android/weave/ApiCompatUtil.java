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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.Window;

/**
 * @author Patrick Woodworth
 */
public abstract class ApiCompatUtil {

  public static final boolean IS_HONEYCOMB =
      android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;

  public static final int SYNC_RUNNING_NOTIFY_ID = 1;

  private static final boolean HIDE_TITLE = false;

  private static ApiCompatUtil sm_instance =
      (IS_HONEYCOMB)
          ? new Api11CompatUtil()
          : new ApiMinCompatUtil();

  public static ApiCompatUtil getInstance() {
    return sm_instance;
  }

  public void postSyncNotification(Context context) {
  }

  public void clearSyncNotification(Context context) {
  }

  public void setupActionBar(Activity activity) {
  }

  public void requestWindowFeatures(Activity activity) {
  }

  public void setWindowFeatures(Activity activity) {
  }

  public static class ApiMinCompatUtil extends ApiCompatUtil {

    @Override
    public void postSyncNotification(Context context) {
      Resources res = context.getResources();

      int icon = R.drawable.stat_notify_sync;
      CharSequence tickerText = res.getString(R.string.sync_notify_ticker);
      long when = System.currentTimeMillis();

      Notification noti = new Notification(icon, tickerText, when);

      CharSequence contentTitle = res.getString(R.string.sync_notify_title);
      CharSequence contentText = res.getString(R.string.sync_notify_text);
      PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);

      noti.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
//      noti.flags |= Notification.FLAG_AUTO_CANCEL;

      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(ns);
      mNotificationManager.notify(SYNC_RUNNING_NOTIFY_ID, noti);
    }

    @Override
    public void clearSyncNotification(Context context) {
      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(ns);
      mNotificationManager.cancel(SYNC_RUNNING_NOTIFY_ID);
    }

    @Override
    public void requestWindowFeatures(Activity activity) {
      activity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    }

    @Override
    public void setWindowFeatures(Activity activity) {
      activity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
    }
  }

  @TargetApi(11)
  public static class Api11CompatUtil extends ApiCompatUtil {

    @Override
    public void setupActionBar(Activity activity) {
      ActionBar actionBar = activity.getActionBar();
      if (HIDE_TITLE) {
        actionBar.setDisplayShowTitleEnabled(false);
      }
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void postSyncNotification(Context context) {
      Resources res = context.getResources();

      PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);

      Notification noti = new Notification.Builder(context.getApplicationContext())
          .setContentTitle(res.getString(R.string.sync_notify_title))
          .setContentText(res.getString(R.string.sync_notify_text))
          .setSmallIcon(R.drawable.sync_anim)
          .setTicker(res.getString(R.string.sync_notify_ticker))
          .setContentIntent(contentIntent)
          .setOngoing(true)
          .getNotification();

      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(ns);
      mNotificationManager.notify(SYNC_RUNNING_NOTIFY_ID, noti);
    }

    @Override
    public void clearSyncNotification(Context context) {
      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(ns);
      mNotificationManager.cancel(SYNC_RUNNING_NOTIFY_ID);
    }
  }
}
