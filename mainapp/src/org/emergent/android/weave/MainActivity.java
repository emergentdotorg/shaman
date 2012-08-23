package org.emergent.android.weave;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.*;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import org.emergent.android.weave.client.WeaveException;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.syncadapter.SyncAssistant;
import org.emergent.android.weave.util.Dbg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Patrick Woodworth
 */
public class MainActivity extends TabActivity implements SyncManager.SyncListener {

  private static final String TAG = Dbg.getTag(MainActivity.class);

  private static final int DEFAULT_TAB = 0;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tabbed_main);

    Resources res = getResources(); // Resource object to get Drawables
    TabHost tabHost = getTabHost();  // The activity TabHost
    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
    Intent intent;  // Reusable Intent for each tab

    intent = new Intent().setClass(this, BookmarkListActivity.class);
    spec = tabHost.newTabSpec("bookmarks")
        .setIndicator("Bookmarks", res.getDrawable(R.drawable.ic_tab_bookmarks))
        .setContent(intent);
    tabHost.addTab(spec);

    intent = new Intent().setClass(this, PasswordListActivity.class);
    spec = tabHost.newTabSpec("passwords")
        .setIndicator("Passwords", res.getDrawable(R.drawable.ic_tab_visited))
        .setContent(intent);
    tabHost.addTab(spec);

    SyncManager.addSyncListener(this);
    String msg = SyncManager.isSyncing() ? "Synchronizing..." : null;
    setNotifyText(msg);

    Resources resources = getResources();
    SharedPreferences p = getSharedPrefs(this);
    DobbyUtil.checkUpgrade(this);
    if (PrefKey.sync_on_open.getBoolean(p, resources)) {
      SyncManager.requestSync(this);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!readInstanceState(this))
      setInitialState();
  }

  @Override
  public void onPause() {
    super.onPause();
    saveData();
  }

  @Override
  protected void onDestroy() {
    SyncManager.removeSyncListener(this);
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    if (Constants.MENUITEM_RESET_DISABLED) {
      MenuItem item = menu.findItem(R.id.reset);
      if (item != null) {
        item.setVisible(false);
      }
    }
    if (Constants.MENUITEM_SETTINGS_DISABLED) {
      MenuItem item = menu.findItem(R.id.settings);
      if (item != null) {
        item.setVisible(false);
      }
    }
    if (Constants.MENUITEM_HELP_DISABLED) {
      MenuItem item = menu.findItem(R.id.help);
      if (item != null) {
        item.setVisible(false);
      }
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.account:
        SyncManager.launchLoginEditor(this);
        return true;
      case R.id.help:
        launchHelp();
        return true;
      case R.id.reset:
        wipeData();
        return true;
      case R.id.resync:
        SyncManager.requestSync(this);
        return true;
      case R.id.settings:
        launchPreferencesEditor();
        return true;
      case R.id.about:
        AboutActivity.showAbout(this);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void launchHelp() {
    Uri uriUrl = Uri.parse(getResources().getString(R.string.help_url));
    Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
    startActivity(intent);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    SyncManager.handleLoginEditorResult(this, requestCode, resultCode, data);
  }

  @Override
  public void syncStatusChanged(SyncManager.SyncEvent event) {
    String msg;
    switch (event.getType()) {
      case FAILED:
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored") Throwable e = event.getThrowable();
        msg = String.format("%s : %s", e.getClass().getSimpleName(), e.getLocalizedMessage());
        if (e instanceof WeaveException) {
          switch (((WeaveException)e).getType()) {
            case UNAUTHORIZED:
              msg = "Sync failed: bad password";
              break;
            case NOTFOUND:
              msg = "Sync failed: bad username";
              break;
            case CRYPTO:
              msg = "Sync failed: bad synckey";
              break;
            default:
              break;
          }
        }
        break;
      default:
        msg = event.isSyncing() ? "Synchronizing..." : null;
        break;
    }
    setNotifyText(msg);
  }

  protected void launchPreferencesEditor() {
    Intent intent = new Intent();
    intent.setClass(this, ApplicationOptionsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  private void wipeData() {
    AlertDialog adb = (new AlertDialog.Builder(this))
        .setTitle(R.string.reset_confirm_title)
        .setMessage(R.string.reset_confirm_message)
        .setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            DobbyUtil.wipeDataImpl(MainActivity.this);
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  protected void setNotifyText(String msg) {
    TextView notify_text = (TextView)findViewById(R.id.notify_text);
    if (notify_text == null) {
      Log.w(TAG, "notify_text was null!");
      return;
    }
    if (msg == null || msg.trim().length() <= 0) {
      msg = "";
    }
    notify_text.setText(msg);
    int visibility = ("".equals(msg)) ? View.GONE : View.VISIBLE;
    notify_text.setVisibility(visibility);
  }

  public void setInitialState() {
    getTabHost().setCurrentTab(DEFAULT_TAB);
  }

  public boolean readInstanceState(Context c) {
    SharedPreferences p = getSharedPrefs(c);
    getTabHost().setCurrentTab(p.getInt(PrefKey.opentab.name(), DEFAULT_TAB));

    // SharedPreferences doesn't fail if the code tries to get a non-existent key. The most straightforward way to
    // indicate success is to return the results of a test that * SharedPreferences contained the position key.
    return (p.contains(PrefKey.opentab.name()));
  }

  private void saveData() {
    // Save the state to the preferences file. If it fails, display a Toast, noting the failure.
    if (!writeInstanceState(this)) {
      Toast.makeText(this, "Failed to write state!", Toast.LENGTH_LONG).show();
    }
  }

  public boolean writeInstanceState(Context c) {
    SharedPreferences p = getSharedPrefs(c);
    SharedPreferences.Editor e = p.edit();
    e.putInt(PrefKey.opentab.name(), getTabHost().getCurrentTab());

    // Commit the changes. Return the result of the commit. The commit fails if Android failed to commit the changes to
    // persistent storage.
    return (e.commit());
  }

  private SharedPreferences getSharedPrefs(Context c) {
    return DobbyUtil.getApplicationPreferences(c);
  }
}
