package org.emergent.android.weave;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.syncadapter.LoginActivity;
import org.emergent.android.weave.syncadapter.SyncAssistant;
import org.emergent.android.weave.util.Dbg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Patrick Woodworth
 */
public class SyncManager {

  private static final ConcurrentLinkedQueue<SyncListener> sm_syncListeners = new ConcurrentLinkedQueue<SyncListener>();

  private static final AtomicReference<AsyncTask<WeaveAccountInfo, Integer, Throwable>> sm_syncThread =
      new AtomicReference<AsyncTask<WeaveAccountInfo, Integer, Throwable>>();

  private static final String TAG = Dbg.getTag(SyncManager.class);

  public static void addSyncListener(SyncListener listener) {
    sm_syncListeners.add(listener);
  }

  public static void removeSyncListener(SyncListener listener) {
    sm_syncListeners.remove(listener);
  }

  public static boolean isSyncing() {
    return sm_syncThread.get() != null;
  }

  public static boolean requestSync(Activity activity) {
    WeaveAccountInfo loginInfo = getLoginInfo(activity);
    if (loginInfo == null) {
      SyncManager.launchLoginEditor(activity);
    } else {
      requestSync(activity, loginInfo);
    }
    return true;
  }

  public static boolean handleLoginEditorResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if (requestCode == Constants.EDIT_ACCOUNT_LOGIN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      SharedPreferences appPrefs = DobbyUtil.getApplicationPreferences(activity);
      SharedPreferences.Editor editor = appPrefs.edit();
      DobbyUtil.intentToLoginPrefs(editor, data);
      boolean updateSaved = editor.commit();
      WeaveAccountInfo loginInfo = DobbyUtil.intentToLogin(data);
      requestSync(activity, loginInfo);
      return true;
    }
    return false;
  }

  public static void launchLoginEditor(Activity activity) {
    Intent intent = new Intent();
    intent.setClass(activity, LoginActivity.class);
    DobbyUtil.loginPrefsToIntent(DobbyUtil.getApplicationPreferences(activity), intent);
    activity.startActivityForResult(intent, Constants.EDIT_ACCOUNT_LOGIN_REQUEST_CODE);
  }

  private static void notifySyncListeners(SyncEvent event) {
//    event = sm_syncThread.get() != null;
    for (SyncListener listener : sm_syncListeners) {
      listener.syncStatusChanged(event);
    }
  }

  private static WeaveAccountInfo getLoginInfo(Context context) {
    WeaveAccountInfo loginInfo = null;
    try {
      Intent intent = new Intent();
      DobbyUtil.loginPrefsToIntent(DobbyUtil.getApplicationPreferences(context), intent);
      loginInfo = DobbyUtil.intentToLogin(intent);
    } catch (Exception e) {
      Log.d(TAG, e.getMessage(), e);
    }
    return loginInfo;
  }

  private static void requestSync(final Context context, WeaveAccountInfo loginInfo) {
    if (loginInfo == null)
      return;

    AsyncTask<WeaveAccountInfo, Integer, Throwable> aTask = new AsyncTask<WeaveAccountInfo, Integer, Throwable>() {
      @Override
      protected Throwable doInBackground(WeaveAccountInfo... accountInfos) {
        WeaveAccountInfo accountInfo = accountInfos[0];
        try {

          final Set<SyncAssistant> syncAssistants = new HashSet<SyncAssistant>(Arrays.asList(
              new SyncAssistant(context, Bookmarks.UPDATER),
              new SyncAssistant(context, Passwords.UPDATER)
          ));

          for (SyncAssistant syncAssistant : syncAssistants) {
            syncAssistant.doQueryAndUpdate(accountInfo.toAuthToken());
          }
        } catch (Throwable e) {
          Log.e(TAG, e.getMessage(), e);
          return e;
        }
        return null;
      }

      @Override
      protected void onPreExecute() {
        notifySyncListeners(new SyncEvent(SyncEventType.STARTED));
      }

      @Override
      protected void onCancelled() {
        notifySyncListeners(new SyncEvent(SyncEventType.CANCELLED));
      }

      @Override
      protected void onProgressUpdate(Integer... values) {
        notifySyncListeners(new SyncEvent(values));
      }

      @Override
      protected void onPostExecute(Throwable e) {
        sm_syncThread.compareAndSet(this, null);
        if (e == null) {
          notifySyncListeners(new SyncEvent(SyncEventType.COMPLETED));
        } else {
          String msg = String.format("sync failed : '%s'", e.getMessage());
          Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
          SyncEvent event = new SyncEvent(e);
          notifySyncListeners(event);
        }
      }
    };


    boolean cmpSetRetval = sm_syncThread.compareAndSet(null, aTask);
    if (cmpSetRetval) {
//      Toast.makeText(context, "starting sync", Toast.LENGTH_LONG).show();
      aTask.execute(loginInfo);
    }
  }

  public static enum SyncEventType {
    STARTED,
    PROGRESS,
    CANCELLED,
    COMPLETED,
    FAILED,
  }

  public static class SyncEvent {

    private final SyncEventType m_type;
    private Throwable m_throwable;

    public SyncEvent(SyncEventType type) {
      m_type = type;
    }

    public SyncEvent(Throwable e) {
      this(SyncEventType.FAILED);
      m_throwable = e;
    }

    public SyncEvent(Integer[] values) {
      this(SyncEventType.PROGRESS);
    }

    public SyncEventType getType() {
      return m_type;
    }

    public Throwable getThrowable() {
      return m_throwable;
    }

    public boolean isSyncing() {
      boolean retval = sm_syncThread.get() != null;
      return retval;
    }
  }

  public static interface SyncListener {

    public void syncStatusChanged(SyncEvent event);
  }
}
