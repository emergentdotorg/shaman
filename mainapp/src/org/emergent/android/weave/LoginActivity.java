/*
 * Copyright 2010 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.emergent.android.weave;

import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.syncadapter.SyncUtil;
import org.emergent.android.weave.util.Dbg.*;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.*;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Activity which displays login screen to the user.
 */
public class LoginActivity extends Activity
    implements AdapterView.OnItemSelectedListener, View.OnClickListener, Constants.Implementable {

  public static final String EXTRA_ALLCERTS = "allcerts";
  public static final String EXTRA_AUTHACTION = "authAction";
  public static final String EXTRA_SERVER = Constants.USERDATA_SERVER_KEY; // "server";
  public static final String EXTRA_USERNAME = Constants.USERDATA_USERNAME_KEY; // "authAccount";
  public static final String EXTRA_PASSWORD = Constants.USERDATA_PASSWORD_KEY; // "password";
  public static final String EXTRA_SECRET = Constants.USERDATA_SECRET_KEY; // "secret";
  public static final String EXTRA_AUTHTOKEN_TYPE = "authtokenType";
  public static final String EXTRA_HINT_MESSAGE = "hintmsg";

  public static final int AUTH_ACTION_UNKNOWN = 0;
  public static final int AUTH_ACTION_ADDACCOUNT = 1;
  public static final int AUTH_ACTION_CONFIRMCREDENTIALS = 2;
  public static final int AUTH_ACTION_UPDATECREDENTIALS = 3;

  private static final int REQ_CUSTOM_SERVER_CODE = 10;

  private static final int DEFAULT_SERVER_SPINNER_POSITION = 1;
  private static final int CUSTOM_SERVER_SPINNER_POSITION = 0;

  private final Handler m_handler = new MyHandler(this);

  private AccountManager m_accountMgr;

  private String m_authtokenType;
  private int m_authAction;

  private TextView m_message;
  private EditText m_serverEdit;
  private EditText m_passwordEdit;
  private EditText m_usernameEdit;
  private EditText m_secretEdit;
//  private CheckBox m_allcertsCheck;

  private int m_spinnerPos;

  private ArrayAdapter<CharSequence> m_serverSpinnerAdapter;

  protected TextWatcher m_filterEditWatcher = new TextWatcher() {

    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
//      handleAuthenticate();
//      if (m_adapter != null)
//        m_adapter.getFilter().filter(s);
    }

  };


  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_LEFT_ICON);
    setContentView(R.layout.login_activity);
    getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_alert);

    m_accountMgr = AccountManager.get(this);

    final Intent intent = getIntent();

    m_authtokenType = intent.getStringExtra(EXTRA_AUTHTOKEN_TYPE);

    m_authAction = intent.getIntExtra(EXTRA_AUTHACTION, AUTH_ACTION_UNKNOWN);

    Spinner spinner = (Spinner)findViewById(R.id.server_spinner);
    m_serverSpinnerAdapter = ArrayAdapter.createFromResource(this,
        R.array.account_setup_login_server_optarray,
        android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(this.m_serverSpinnerAdapter);
    spinner.setOnItemSelectedListener(this);

    m_message = (TextView)findViewById(R.id.message);

    m_serverEdit = (EditText)findViewById(R.id.serverurl_edit);
    m_usernameEdit = (EditText)findViewById(R.id.username_edit);
    m_passwordEdit = (EditText)findViewById(R.id.password_edit);
    m_secretEdit = (EditText)findViewById(R.id.secret_edit);
//    m_allcertsCheck = (CheckBox)findViewById(R.id.allcerts_checkbox);

    initWidgetsFromIntent(intent);
    m_message.setText(getMessage());


//    m_usernameEdit.addTextChangedListener(m_filterEditWatcher);

    Button button;

    button = (Button)findViewById(R.id.next_button);
    button.setOnClickListener(this);
    button = (Button)findViewById(R.id.test_button);
    button.setOnClickListener(this);
    Properties defs = Constants.getRuntimeDefaults();
    if (!defs.isEmpty()) {
      button.setText(R.string.test);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (m_usernameEdit != null) {
      m_usernameEdit.removeTextChangedListener(m_filterEditWatcher);
    }
    super.onDestroy();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    final ProgressDialog dialog = new ProgressDialog(this);
    dialog.setMessage(getText(R.string.ui_activity_authenticating));
    dialog.setIndeterminate(true);
    dialog.setCancelable(true);
    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        Log.d(TAG, "dialog cancel has been invoked");
        cancelAuthTask();
      }
    });
    return dialog;
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.next_button:
        handleAuthenticate();
        break;
      case R.id.test_button:
        Properties defs = Constants.getRuntimeDefaults();
        if (!defs.isEmpty()) {
          initWidgetTestData();
        } else {
          handleCancelAndClose();
        }
        break;
    }
  }

  private void handleSaveAndClose(AuthResult authResult) {
    final Intent intent = new Intent();
    WeaveAccountInfo loginInfo = authResult.m_info;
    StaticUtils.loginInfoToIntent(loginInfo, intent);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
    setResult(RESULT_OK, intent);
    finish();
  }

  private void handleCancelAndClose() {
    setResult(RESULT_CANCELED);
    finish();
  }


  /**
   * When the user selects an item in the spinner, this method is invoked by the callback chain. Android calls the item
   * selected listener for the spinner, which invokes the onItemSelected method.
   *
   * @param parent - the AdapterView for this listener
   * @param v      - the View for this listener
   * @param pos    - the 0-based position of the selection in the mLocalAdapter
   * @param row    - the 0-based row number of the selection in the View
   * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android.widget.AdapterView, android.view.View, int, long)
   */
  public void onItemSelected(AdapterView<?> parent, View v, int pos, long row) {
    m_spinnerPos = pos;
//    m_spinnerSelection = parent.getItemAtPosition(pos).toString();
    updateEditVisibility(pos);
  }

  public void onNothingSelected(AdapterView<?> parent) {
    // do nothing
  }

  private void cancelAuthTask() {
//    if (mAuthThread != null) {
//      mAuthThread.cancel(true);
//      mAuthThread = null;
      finish();
//    }
  }

  /**
   * Handles onClick event on the Submit button. Sends username/password to the server for authentication.
   */
  public void handleAuthenticate() {
    Log.d(TAG, "AuthenticatorActivity.handleAuthenticate() m_authAction = " + m_authAction);
    final WeaveAccountInfo loginInfo = createWeaveAccountInfoValidating();
    if (loginInfo == null) {
      // The hint message should already be updated
      return;
    }

    showProgress();
    // Start authenticating...

    SyncUtil.requestAuth(this, loginInfo, m_handler);
  }

  private void onAuthenticationResult(final AuthResult authResult) {
    final WeaveAccountInfo loginInfo = authResult.m_info;
    Log.d(TAG, String.format("onAuthenticationResult() result = %s ; action = %s", authResult.m_success, m_authAction));
    hideProgress();
    if (!authResult.m_success || loginInfo == null) {
      DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
              handleSaveAndClose(authResult);
              break;
            case DialogInterface.BUTTON_NEGATIVE:
              updateHintMessage();
              break;
          }
        }
      };

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage("Login test failed, save changes anyway?")
          .setPositiveButton("Yes", clickListener)
          .setNegativeButton("No", clickListener)
          .show();

    } else {
      handleSaveAndClose(authResult);
    }
  }

  /**
   * Hides the progress UI for a lengthy operation.
   */
  protected void hideProgress() {
//    dismissDialog(0);
  }

  /**
   * Shows the progress UI for a lengthy operation.
   */
  protected void showProgress() {
//    showDialog(0);
  }

  private String getServer() {
    String retval = null;
    if (m_spinnerPos == CUSTOM_SERVER_SPINNER_POSITION) {
      EditText serverEdit = (EditText)findViewById(R.id.serverurl_edit);
      retval = serverEdit.getText().toString();
    } else {
      Resources res = getResources();
      retval = res.getStringArray(R.array.account_setup_login_server_optarray_values)[m_spinnerPos];
    }
    return retval;
  }

  private void initWidgetsFromIntent(Intent intent) {
    initWidgetsFromIntent(this, R.id.serverurl_edit, EXTRA_SERVER, intent);
    initWidgetsFromIntent(this, R.id.username_edit, EXTRA_USERNAME, intent);
    initWidgetsFromIntent(this, R.id.password_edit, EXTRA_PASSWORD, intent);
    initWidgetsFromIntent(this, R.id.secret_edit, EXTRA_SECRET, intent);
//    initWidgetsFromIntent(this, R.id.allcerts_checkbox, EXTRA_ALLCERTS, intent);
    updateSpinner();
  }

  private void updateSpinner() {
    EditText serverEdit = (EditText)findViewById(R.id.serverurl_edit);
    String serverValue = serverEdit.getText().toString();
    int spinnerPos = CUSTOM_SERVER_SPINNER_POSITION;
    if (TextUtils.isEmpty(serverValue)) {
      spinnerPos = DEFAULT_SERVER_SPINNER_POSITION;
    } else {
      Resources res = getResources();
      String[] values = res.getStringArray(R.array.account_setup_login_server_optarray_values);
      for (int ii = 0; ii < values.length; ii++) {
        if (values[ii].equals(serverValue))
          spinnerPos = ii;
      }
    }
    Spinner restoreSpinner = (Spinner)findViewById(R.id.server_spinner);
    restoreSpinner.setSelection(spinnerPos);
//    m_spinnerPos = spinnerPos;
  }

  private void updateEditVisibility(int pos) {
    EditText serverEdit = (EditText)findViewById(R.id.serverurl_edit);
    switch (pos) {
      case 0:
        serverEdit.setVisibility(View.VISIBLE);
        break;
      default:
        serverEdit.setVisibility(View.GONE);
    }
  }

  static void initWidgetsFromIntent(Activity activity, int resId, String key, Intent intent) {
    View view = activity.findViewById(resId);
    if (intent.hasExtra(key)) {
      if (view instanceof EditText) {
        EditText edit = (EditText)view;
        edit.setText(intent.getStringExtra(key));
      } else if (view instanceof CheckBox) {
        CheckBox edit = (CheckBox)view;
        edit.setChecked(intent.getBooleanExtra(key, edit.isChecked()));
      }
    }
  }

  private void initWidgetTestData() {
    Intent intent = new Intent();
    Properties defs = Constants.getRuntimeDefaults();
    initWidgetTestData(defs, EXTRA_SERVER, intent, false);
    initWidgetTestData(defs, EXTRA_USERNAME, intent, false);
    initWidgetTestData(defs, EXTRA_PASSWORD, intent, false);
    initWidgetTestData(defs, EXTRA_SECRET, intent, false);
    initWidgetTestData(defs, EXTRA_ALLCERTS, intent, true);
    initWidgetsFromIntent(intent);
  }

  private static void initWidgetTestData(Properties defs, String key, Intent intent, boolean isBoolean) {
    String keyReprefixed = "login." + key;
    String def;
    if ((def = defs.getProperty(keyReprefixed)) != null) {
      if (isBoolean) {
        intent.putExtra(key, Boolean.parseBoolean(def));
      } else {
        intent.putExtra(key, def);
      }
    }
  }

  private void updateHintMessage() {
    if (m_authAction == AUTH_ACTION_ADDACCOUNT) {
      // "Please enter a valid username/password.
      m_message.setText(getText(R.string.login_activity_loginfail_text_both));
    } else {
      // "Please enter a valid password." (Used when the
      // account is already in the database but the password
      // doesn't work.)
      m_message.setText(getText(R.string.login_activity_loginfail_text_pwonly));
    }
  }

  /**
   * Returns the message to be displayed at the top of the login dialog box.
   *
   * @return message to be displayed
   */
  private CharSequence getMessage() {
    getString(R.string.account_label);
    if (TextUtils.isEmpty(m_usernameEdit.getText().toString())) {
      // If no username, then we ask the user to log in using an appropriate service.
      return getText(R.string.login_activity_newaccount_text);
    }
    if (TextUtils.isEmpty(m_passwordEdit.getText().toString())) {
      // We have an account but no password
      return getText(R.string.login_activity_loginfail_text_pwmissing);
    }
    if (TextUtils.isEmpty(m_secretEdit.getText().toString())) {
      // We have an account but no secret
      return getText(R.string.login_activity_loginfail_text_secretmissing);
    }
    return null;
  }

  private WeaveAccountInfo createWeaveAccountInfoValidating() {
    String serverUri = getServer();
    String username = m_usernameEdit.getText().toString();
    String password = m_passwordEdit.getText().toString();
    String secret = m_secretEdit.getText().toString();
    boolean allcerts = true; // m_allcertsCheck.isChecked();
    Log.d(TAG, "Authenticating: " + username + " : " + password + " @ " + serverUri);
    WeaveAccountInfo retval = createWeaveInfo(serverUri, username, password, secret);
    final SharedPreferences prefs = StaticUtils.getAccountPreferences(LoginActivity.this, retval);
    prefs.edit().putBoolean(PrefKey.allcerts.name(), allcerts).commit();
    return retval;
  }

  private WeaveAccountInfo createWeaveInfo(String serverUri, String username, String password, String secret) {
    URI server;
    try {
      server = new URI(serverUri);
    } catch (URISyntaxException e) {
      m_message.setText(getText(R.string.login_activity_loginfail_text_server));
      return null;
    }
    WeaveAccountInfo retval = null;
    try {
      retval = WeaveAccountInfo.createWeaveAccountInfo(server, username, password, secret.toCharArray());
    } catch (Exception ignored) {
    }
    if (retval == null) {
      m_message.setText(getText(R.string.login_activity_loginfail_text_unknown));
      return null;
    }
    return retval;
  }

  private void updateSyncStatusText(Message message) {
    AuthResult result = null;
    Object obj = message.obj;
    WeaveAccountInfo accountInfo = StaticUtils.bundleToLogin((Bundle)obj);
    SyncEventType eventType = SyncEventType.valueOf(message.arg2);
    switch (eventType) {
      case COMPLETED:
        result = new AuthResult(true, accountInfo, null, null);
        break;
      case FAILED:
      default:
        result =  new AuthResult(false, accountInfo, "auth failed", null);
        break;
    }
    onAuthenticationResult(result);
  }

  private static class AuthResult {

    public final boolean m_success;
    public final WeaveAccountInfo m_info;
    public final String m_message;
    public final Throwable m_thrown;

    public AuthResult(boolean success, WeaveAccountInfo info, String message, Throwable thrown) {
      m_success = success;
      m_info = info;
      m_message = message;
      m_thrown = thrown;
    }
  }

  private static class MyHandler extends Handler {

    private final WeakReference<LoginActivity> m_activityRef;

    public MyHandler(LoginActivity activity) {
      m_activityRef = new WeakReference<LoginActivity>(activity);
    }

    @Override
    public void handleMessage(Message message) {
      if (message.arg1 == Constants.SYNC_EVENT) {
        LoginActivity activity = m_activityRef.get();
        if (activity == null)
          return;
        activity.updateSyncStatusText(message);
      }
    }
  }
}

