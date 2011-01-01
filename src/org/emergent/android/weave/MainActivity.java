package org.emergent.android.weave;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;
import org.emergent.android.weave.client.WeaveAccountInfo;

/**
 * @author Patrick Woodworth
 */
public class MainActivity extends TabActivity {

  private static final int DEFAULT_TAB = 0;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!EulaActivity.checkEula(this)) {
      return;
    }

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

/*
    intent = new Intent().setClass(this, TabListActivity.class);
    spec = tabHost.newTabSpec("tabs")
        .setIndicator("Tabs", res.getDrawable(R.drawable.ic_tab_contacts))
        .setContent(intent);
    tabHost.addTab(spec);

    intent = new Intent().setClass(this, TabListActivity.class);
    spec = tabHost.newTabSpec("debug")
        .setIndicator("Debug", res.getDrawable(R.drawable.ic_tab_recent))
        .setContent(intent);
    tabHost.addTab(spec);

*/
    WeaveAccountInfo loginInfo = AbstractListActivity.getLoginInfo(this);
    if (loginInfo == null) {
    } else {
      AbstractListActivity.requestSync(this);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!readInstanceState(this)) setInitialState();
  }

  @Override
  public void onPause() {
    super.onPause();
    saveData();
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
