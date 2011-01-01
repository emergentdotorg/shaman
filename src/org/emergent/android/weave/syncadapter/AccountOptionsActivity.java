/*
 * Copyright 2010 Patrick Woodworth
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

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import org.emergent.android.weave.DobbyUtil;
import org.emergent.android.weave.PrefKey;
import org.emergent.android.weave.R;
import org.emergent.android.weave.client.WeaveConstants;
import org.emergent.android.weave.util.Dbg;

/**
 * @author Patrick Woodworth
 */
public class AccountOptionsActivity extends Activity
    implements CompoundButton.OnCheckedChangeListener, View.OnClickListener
{

  private static final String TAG = Dbg.getTag(AccountOptionsActivity.class);

  private Account m_account;
  private boolean m_allcerts;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.account_options);
    Intent intent = getIntent();
    if (intent != null && intent.hasExtra("account"))
      m_account = intent.getParcelableExtra("account");

    CheckBox allcertsCheck = (CheckBox)findViewById(R.id.allcerts_checkbox);
    allcertsCheck.setOnCheckedChangeListener(this);

    Button button;
    button = (Button)findViewById(R.id.save_button);
    button.setOnClickListener(this);
    button = (Button)findViewById(R.id.cancel_button);
    button.setOnClickListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();

    // Try to read the preferences file. If not found, set the state to the desired initial
    if (!readInstanceState(this)) setInitialState();

    CheckBox allcertsCheck = (CheckBox)findViewById(R.id.allcerts_checkbox);
    allcertsCheck.setChecked(m_allcerts);
  }

  /**
   * Store the current state of the widgets. Since onPause() is always called when an Activity is about to be hidden,
   * even if it is about to be destroyed, it is the best place to save state.
   * <p/>
   * Attempt to write the state to the preferences file. If this fails, notify the user.
   */
  @Override
  public void onPause() {
    super.onPause();
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.save_button:
        saveData();
      case R.id.cancel_button:
        finish();
        break;
    }
  }


  public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
    m_allcerts = checked;
  }

  public void setInitialState() {
    m_allcerts = WeaveConstants.ALLOW_INVALID_CERTS_DEFAULT;
  }

  public boolean readInstanceState(Context c) {
    SharedPreferences p = getSharedPrefs(c);
    m_allcerts = p.getBoolean(PrefKey.allcerts.name(), WeaveConstants.ALLOW_INVALID_CERTS_DEFAULT);

    // SharedPreferences doesn't fail if the code tries to get a non-existent key. The most straightforward way to
    // indicate success is to return the results of a test that * SharedPreferences contained the position key.
    return (p.contains(PrefKey.allcerts.name()));
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
    e.putBoolean(PrefKey.allcerts.name(), m_allcerts);

    // Commit the changes. Return the result of the commit. The commit fails if Android failed to commit the changes to
    // persistent storage.
    return (e.commit());
  }

  private SharedPreferences getSharedPrefs(Context c) {
    return DobbyUtil.getAccountPreferences(c, m_account);
  }
}
