package org.emergent.android.weave;

import android.app.*;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.syncadapter.LoginActivity;
import org.emergent.android.weave.syncadapter.SyncAssistant;
import org.emergent.android.weave.util.Dbg;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Patrick Woodworth
 */
public abstract class AbstractListActivity extends ListActivity {

  private static final String TAG = Dbg.getTag(AbstractListActivity.class);

  private static final int EDIT_ACCOUNT_LOGIN_REQUEST = 1000;

  protected EditText m_filterEdit = null;
  protected SimpleCursorAdapter m_adapter = null;

  private static final AtomicReference<AsyncTask<WeaveAccountInfo, Integer, Throwable>> sm_syncThread =
      new AtomicReference<AsyncTask<WeaveAccountInfo, Integer, Throwable>>();

  protected final AtomicReference<CharSequence> m_filterString = new AtomicReference<CharSequence>();

  protected TextWatcher m_filterEditWatcher = new TextWatcher() {

    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
      m_filterString.set(s);
      validateFilter();
    }

  };

  protected void validateFilter() {
    if (m_adapter != null)
      m_adapter.getFilter().filter(m_filterString.get());
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (m_filterEdit != null) {
      m_filterEdit.removeTextChangedListener(m_filterEditWatcher);
    }
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.account:
        launchAccountEditor();
        return true;
      case R.id.reset:
        wipeData();
        return true;
      case R.id.resync:
        requestSync();
        return true;
      case R.id.settings:
        launchPreferencesEditor();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == EDIT_ACCOUNT_LOGIN_REQUEST && resultCode == RESULT_OK) {
      SharedPreferences appPrefs = DobbyUtil.getApplicationPreferences(this);
      SharedPreferences.Editor editor = appPrefs.edit();
      DobbyUtil.intentToLoginPrefs(editor, data);
      boolean updateSaved = editor.commit();
//      String msg = String.format("updateSaved : '%s'", updateSaved);
//      Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
      requestSync();
    }
//    if (requestCode == EDIT_ACCOUNT_LOGIN_REQUEST && resultCode != RESULT_OK) {
//      Toast.makeText(this, "update cancelled", Toast.LENGTH_LONG).show();
//    }
  }

  protected void launchPreferencesEditor() {
    Intent intent = new Intent();
    intent.setClass(this, ApplicationOptionsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  protected void launchAccountEditor() {
    Intent intent = new Intent();
    intent.setClass(this, LoginActivity.class);
    DobbyUtil.loginPrefsToIntent(DobbyUtil.getApplicationPreferences(this), intent);
    startActivityForResult(intent, EDIT_ACCOUNT_LOGIN_REQUEST);
  }

  private void requestSync() {
    requestSync(this);
  }

  static void requestSync(final Context context) {
    WeaveAccountInfo loginInfo = null;

    try {
      Intent intent = new Intent();
      DobbyUtil.loginPrefsToIntent(DobbyUtil.getApplicationPreferences(context), intent);
      loginInfo = DobbyUtil.intentToLogin(intent);
    } catch (Exception e) {
      Log.d(TAG, e.getMessage(), e);
    }

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
      protected void onProgressUpdate(Integer... values) {
      }

      @Override
      protected void onPostExecute(Throwable e) {
        sm_syncThread.compareAndSet(this, null);
        if (e == null)
          return;
        String msg = String.format("sync failed : '%s'", e.getMessage());
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
      }
    };

    boolean cmpSetRetval = sm_syncThread.compareAndSet(null, aTask);
    if (cmpSetRetval) {
      Toast.makeText(context, "starting sync", Toast.LENGTH_LONG).show();
      aTask.execute(loginInfo);
    }
  }

//  private static final int HELLO_ID = 1;
//
//  static void setNewMessageIndicator(Context context, int messageCount){
//
//    String ns = Context.NOTIFICATION_SERVICE;
//    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
//
//    int icon = R.drawable.stat_sys_warning;
//    CharSequence tickerText = "Hello";
//    long when = System.currentTimeMillis();
//
//    Notification notification = new Notification(icon, tickerText, when);
//
////    Context context = getApplicationContext();
//    CharSequence contentTitle = "My notification";
//    CharSequence contentText = "Hello World!";
//    Intent notificationIntent = new Intent(this, AbstractListActivity.class);
//    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
//
//    notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
//
//    mNotificationManager.notify(HELLO_ID, notification);
//
////   // If we're being called because a new message has been received,
////   // then display an icon and a count. Otherwise, delete the persistent
////   // message.
////   if (messageCount > 0) {
////      nm.notifyWithText(myApp.NOTIFICATION_GUID,      // ID for this notification.
////                messageCount + " new message" + messageCount > 1 ? "s":"", // Text to display.
////                NotificationManager.LENGTH_SHORT); // Show it for a short time only.
//   }


  private void wipeData() {
    Log.w(TAG, "wipeData");
    ContentResolver resolver = getContentResolver();
    Passwords.UPDATER.deleteRecords(resolver);
    Bookmarks.UPDATER.deleteRecords(resolver);
  }

  protected void showMyDialog(String title, String msg) {
    AlertDialog.Builder adb = new AlertDialog.Builder(this);
    adb.setTitle(title);
//    Log.v(TAG, msg);
    adb.setMessage(msg);
    adb.setPositiveButton("Ok", null);
    adb.show();
  }

  protected void setupAdapter() {
    ContentResolver cr = getContentResolver();
    // Get the list view
//    final ListView listView = (ListView)findViewById(R.id.listView);
//    final ListView listView = this.getListView();
    FilterQueryProvider qfp = getQueryFilterProvider(cr);
    m_adapter = createCursorAdapter(qfp);
    m_adapter.setFilterQueryProvider(qfp);
//    listView.setAdapter(adapter);
    setListAdapter(m_adapter);
    m_filterEdit = (EditText) findViewById(R.id.search_box);
    m_filterEdit.addTextChangedListener(m_filterEditWatcher);
  }

  protected abstract SimpleCursorAdapter createCursorAdapter(FilterQueryProvider qfp);

  protected abstract FilterQueryProvider getQueryFilterProvider(ContentResolver cr);

  protected class MyViewBinder implements SimpleCursorAdapter.ViewBinder {

    private final SimpleDateFormat m_dateFormat = new SimpleDateFormat();

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
      if (view instanceof TextView &&
          Weaves.Columns.LAST_MODIFIED.equals(cursor.getColumnName(columnIndex))) {
        TextView tview = (TextView) view;
        long theDateLong = cursor.getLong(columnIndex);
        Date theDate = new Date(theDateLong * 1000);
//        GregorianCalendar sm_gregorianCalendar = new GregorianCalendar();
//        sm_gregorianCalendar.setTimeInMillis(theDateLong);
        String dateStr = m_dateFormat.format(theDate);
//        String dateStr = "" + theDate.toString();
//        String dateStr = "" + theDateLong;
        tview.setText(dateStr);
        return true;
      }
      return false;
    }
  }

  protected class MyCursorAdapter extends SimpleCursorAdapter {

    private Context context;
    private int layout;

    public MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
      this(new MyViewBinder(), context, layout, c, from, to);
    }

    public MyCursorAdapter(ViewBinder binder, Context context, int layout, Cursor c, String[] from, int[] to) {
      super(context, layout, c, from, to);
      this.context = context;
      this.layout = layout;
      this.setViewBinder(binder);
    }

    @Override
    public int getStringConversionColumn() {
      return super.getStringConversionColumn();
    }

    @Override
    public void setStringConversionColumn(int stringConversionColumn) {
      super.setStringConversionColumn(stringConversionColumn);
    }

    @Override
    public Object getItem(int position) {
      return super.getItem(position);
    }
  }

}
