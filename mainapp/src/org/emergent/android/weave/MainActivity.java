package org.emergent.android.weave;

import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.syncadapter.SyncUtil;
import org.emergent.android.weave.util.Dbg.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * @author Patrick Woodworth
 */
public class MainActivity extends FragmentActivity implements FragUtils.FragmentDataStore, Constants.Implementable {

  private static final int DEFAULT_TAB = 0;

  private final Handler m_handler = new MyHandler(this);

  private final Bundle m_fragData = new Bundle();

  public MainActivity() {
    Log.v(TAG, getClass().getSimpleName() + ".<init> (" + hashCode() + ")");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, getClass().getSimpleName() + ".onCreate (" + hashCode() + "): " + (savedInstanceState != null));

    ApiCompatUtil compatUtil = ApiCompatUtil.getInstance();
    compatUtil.requestWindowFeatures(this);
    setContentView(R.layout.prime);
    compatUtil.setWindowFeatures(this);

    compatUtil.setupActionBar(this);

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

    boolean upgraded = false;
    if (savedInstanceState != null) {
//      setCurrentTab(savedInstanceState.getInt(PrefKey.opentab.name(), -1));
    } else {
      Resources resources = getResources();
      SharedPreferences p = StaticUtils.getApplicationPreferences(this);
      upgraded = StaticUtils.checkUpgrade(this);
      if (!readInstanceState(this))
        setInitialState();
      if (PrefKey.sync_on_open.getBoolean(p, resources)) {
        WeaveAccountInfo loginInfo = StaticUtils.getLoginInfo(this);
        if (upgraded || loginInfo == null) {
          StaticUtils.requestSync(this, m_handler);
        }
      }
    }

    FragmentManager fm = getSupportFragmentManager();
    // You can find Fragments just like you would with a View by using FragmentManager.
    Fragment fragment = fm.findFragmentById(R.id.fragment_content);

    // If we are using activity_fragment_xml.xml then this the fragment will not be null, otherwise it will be.
    if (fragment == null) {
      setMyFragment(new MiscListFragment(), false);
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
    if (Constants.MENUITEM_HOME_DISABLED) {
      MenuItem item = menu.findItem(R.id.home);
      if (item != null) {
        item.setVisible(false);
      }
    }
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
      case R.id.home:
        FragUtils.popFragmentViewDelayed(getSupportFragmentManager());
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
      SyncUtil.requestSync(this, loginInfo, m_handler);
    }
  }

  public void setMyFragment(Fragment fragment) {
    setMyFragment(fragment, true);
  }

  public void setMyFragment(Fragment fragment, boolean addToBackStack) {
    FragmentManager fm = getSupportFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    ft.replace(R.id.fragment_content, fragment);
    if (addToBackStack)
      ft.addToBackStack(null);
    ft.commit();
  }

  @Override
  public void onBackPressed() {
    boolean handled = false;
    try {
      FragmentManager fm = getSupportFragmentManager();
      Fragment frag = fm.findFragmentById(R.id.fragment_content);
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
    return DEFAULT_TAB;
  }

  protected void setCurrentTab(int idx) {
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
