package org.emergent.android.weave;

import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.util.Dbg.*;
import org.emergent.android.weave.util.FragmentTabListener;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * @author Patrick Woodworth
 */
public class MainActivity extends Activity implements Constants.Implementable, FragUtils.FragmentDataStore {

  private static final int DEFAULT_TAB = 0;

  public static final boolean HIDE_TITLE = false;
  public static final boolean USE_TAB_ICONS = false;

  private final Handler m_handler = new MyHandler(this);

  private final Bundle m_fragData = new Bundle();

  public MainActivity() {
    Log.v(TAG, getClass().getSimpleName() + ".<init> (" + hashCode() + ")");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, getClass().getSimpleName() + ".onCreate (" + hashCode() + "): " + (savedInstanceState != null));
    setContentView(R.layout.prime);

    // setup action bar for tabs
    ActionBar actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    if (HIDE_TITLE) {
      actionBar.setDisplayShowTitleEnabled(false);
    }

    int curTab = DEFAULT_TAB;

    if (savedInstanceState != null) {
      Bundle fragData = savedInstanceState.getBundle("fragData");
      if (fragData != null) {
        Log.d(TAG, "MainActivity.onCreate: got fragData!");
        for (String key : fragData.keySet()) {
          Log.d(TAG, "  fragData key: " + key);
          if (!m_fragData.containsKey(key))
            m_fragData.putBundle(key, fragData.getBundle(key));
        }
      }

      curTab = savedInstanceState.getInt(PrefKey.opentab.name(), DEFAULT_TAB);
    }

    createAndAddTab(R.string.bookmarks, BookmarkListFragment.class, R.drawable.ic_tab_bookmarks, curTab);
    createAndAddTab(R.string.passwords, PasswordListFragment.class, R.drawable.ic_tab_visited, curTab);

    if (savedInstanceState != null) {
//      setCurrentTab(savedInstanceState.getInt(PrefKey.opentab.name(), -1));
    } else {
      Resources resources = getResources();
      SharedPreferences p = StaticUtils.getApplicationPreferences(this);
      StaticUtils.checkUpgrade(this);

      if (!readInstanceState(this))
        setInitialState();
      if (PrefKey.sync_on_open.getBoolean(p, resources)) {
        StaticUtils.requestSync(this, m_handler);
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    Log.v(TAG, getClass().getSimpleName() + ".onSaveInstanceState (" + hashCode() + ")");
    super.onSaveInstanceState(outState);
    outState.putInt(PrefKey.opentab.name(), getCurrentTab());
    outState.putBundle("fragData", m_fragData);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    Log.v(TAG, getClass().getSimpleName() + ".onRestoreInstanceState (" + hashCode() + "): " + (savedInstanceState != null));
    super.onRestoreInstanceState(savedInstanceState);
    if (savedInstanceState == null)
      return;

    Bundle fragData = savedInstanceState.getBundle("fragData");
    if (fragData != null) {
      for (String key : fragData.keySet()) {
        if (!m_fragData.containsKey(key))
          m_fragData.putBundle(key, fragData.getBundle(key));
      }
    }
    int curTab = savedInstanceState.getInt(PrefKey.opentab.name(), -1);
    if (curTab != getCurrentTab()) {
      setCurrentTab(curTab);
    }
  }

  @Override
  protected void onResume() {
    Log.v(TAG, getClass().getSimpleName() + ".onResume (" + hashCode() + ")");
    super.onResume();
  }

  @Override
  public void onPause() {
    Log.v(TAG, getClass().getSimpleName() + ".onPause (" + hashCode() + ")");
    saveData();
    super.onPause();
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
      case android.R.id.home:
        FragUtils.popFragmentViewDelayed(getFragmentManager());
        return true;
      case R.id.account:
        StaticUtils.launchLoginEditor(this);
        return true;
      case R.id.help:
        StaticUtils.launchHelp(this);
        return true;
      case R.id.reset:
        StaticUtils.wipeData(this);
        return true;
      case R.id.resync:
        StaticUtils.requestSync(this, m_handler);
        return true;
      case R.id.settings:
        StaticUtils.launchPreferencesEditor(this);
        return true;
      case R.id.about:
        AboutActivity.showAbout(this);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == Constants.EDIT_ACCOUNT_LOGIN_REQUEST_CODE) {
      if (resultCode != RESULT_OK) {
        return;
      }
      SharedPreferences appPrefs = StaticUtils.getApplicationPreferences(this);
      SharedPreferences.Editor editor = appPrefs.edit();
      StaticUtils.intentToLoginPrefs(editor, data);
      boolean updateSaved = editor.commit();
      WeaveAccountInfo loginInfo = StaticUtils.intentToLogin(data);
      StaticUtils.requestSync(this, loginInfo, m_handler);
    }
  }

  @Override
  public void onBackPressed() {
    boolean handled = false;
    try {
      ActionBar actionBar = getActionBar();
      if (actionBar == null) {
        Log.d(TAG, "actionBar was null!");
        return;
      }

      ActionBar.Tab tab = actionBar.getSelectedTab();
      if (tab == null) {
        Log.d(TAG, "tab was null!");
        return;
      }

      Object tag = tab.getTag();
      if (tag != null)
        tag = tag.toString();

      if (!(tag instanceof String)) {
        Log.d(TAG, "tag type was: " + (tag == null ? "null" : tag.getClass().getSimpleName()));
        return;
      }

      FragmentManager fm = getFragmentManager();
      Fragment frag = fm.findFragmentByTag((String)tag);

      if (!(frag instanceof FragUtils.BackPressedHandler)) {
        Log.d(TAG, "frag type was: " + (frag == null ? "null" : frag.getClass().getSimpleName()));
        return;
      }

      if (!frag.isVisible()) {
        Log.d(TAG, "frag was not visible!");
        return;
      }

      handled = ((FragUtils.BackPressedHandler)frag).handleBackPressed();
//      Log.w(TAG, "handleBackPressed returned: " + handled);
    } catch (Exception e) {
      Log.e(TAG, "Could not check onBackPressed", e);
    } finally {
      if (!handled) {
        super.onBackPressed();
      }
    }
  }

  @Override
  public Bundle getFragData(String tag) {
    return m_fragData.getBundle(tag);
  }

  @Override
  public void setFragData(String tag, Bundle bundle) {
    m_fragData.putBundle(tag, bundle);
  }

  private void setInitialState() {
//    setCurrentTab(DEFAULT_TAB);
  }

  private boolean readInstanceState(Context c) {
    SharedPreferences p = StaticUtils.getApplicationPreferences(c);
//    setCurrentTab(p.getInt(PrefKey.opentab.name(), DEFAULT_TAB));

    // SharedPreferences doesn't fail if the code tries to get a non-existent key. The most straightforward way to
    // indicate success is to return the results of a test that * SharedPreferences contained the position key.
    return (p.contains(PrefKey.lastPrefSave.name()));
  }

  private void saveData() {
    // Save the state to the preferences file. If it fails, display a Toast, noting the failure.
    if (!writeInstanceState(this)) {
      Toast.makeText(this, "Failed to write state!", Toast.LENGTH_LONG).show();
    }
  }

  private boolean writeInstanceState(Context c) {
    SharedPreferences p = StaticUtils.getApplicationPreferences(c);
    SharedPreferences.Editor e = p.edit();
    e.putLong(PrefKey.lastPrefSave.name(), System.currentTimeMillis());
    e.putInt(PrefKey.opentab.name(), getCurrentTab());

    // Commit the changes. Return the result of the commit. The commit fails if Android failed to commit the changes to
    // persistent storage.
    return (e.commit());
  }

  protected int getCurrentTab() {
    ActionBar actionBar = getActionBar();
    int retval = actionBar.getSelectedNavigationIndex();
    Log.d(TAG, "getCurrentTab: " + retval);
    return retval;
  }

  protected void setCurrentTab(int idx) {
    if (false)
      return;
    Log.d(TAG, "setCurrentTab: " + idx);
    ActionBar actionBar = getActionBar();
    int tabCnt = actionBar.getTabCount();
    if (idx < 0 || idx >= tabCnt)
      return;
    ActionBar.Tab tab = actionBar.getTabAt(idx);
    actionBar.selectTab(tab);
  }

  private <T extends Fragment> void createAndAddTab(int nameId, Class<T> clazz, int iconId, int curTab) {
    Resources res = getResources();
    ActionBar actionBar = getActionBar();
    String tagStr = res.getString(nameId);
    Bundle args = new Bundle();
    args.putString(Constants.Implementable.FRAG_TAG_BUNDLE_KEY, tagStr);
    ActionBar.Tab tab = actionBar.newTab()
        .setText(nameId)
        .setTag(tagStr)
        .setTabListener(new FragmentTabListener<T>(this, R.id.fragment_content, tagStr, clazz, args))
        ;

    if (iconId > 0 && MainActivity.USE_TAB_ICONS) {
      tab.setIcon(res.getDrawable(iconId));
    }

    int pos = actionBar.getTabCount();
    actionBar.addTab(tab, pos, pos == curTab);
  }

  private void updateSyncStatusText(Message message) {
    String msgtxt = null;
    SyncEventType eventType = SyncEventType.valueOf(message.arg2);
    switch (eventType) {
      case STARTED:
        msgtxt = "Synchronizing...";
        break;
      case BAD_USERNAME:
        msgtxt = "Sync failed: bad username!";
        break;
      case BAD_PASSWORD:
        msgtxt = "Sync failed: bad password!";
        break;
      case BAD_SYNCKEY:
        msgtxt = "Sync failed: bad sync key!";
        break;
      case FAILED:
        Object obj = message.obj;
        String msg = obj != null ? obj.toString() : null;
        msgtxt = "Sync failed: " + msg;
        break;
      default:
        // use default
    }
    setNotifyText(msgtxt);
  }

  private void setNotifyText(String msg) {
    if (!TextUtils.isEmpty(msg))
      Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
  }

  private static class MyHandler extends Handler {

    private final WeakReference<MainActivity> m_activityRef;

    public MyHandler(MainActivity activity) {
      m_activityRef = new WeakReference<MainActivity>(activity);
    }

    @Override
    public void handleMessage(Message message) {
      if (message.arg1 == Constants.SYNC_EVENT) {
        MainActivity activity = m_activityRef.get();
        if (activity == null)
          return;
        activity.updateSyncStatusText(message);
      }
    }
  }
}
