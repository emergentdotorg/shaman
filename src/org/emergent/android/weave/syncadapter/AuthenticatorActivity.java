package org.emergent.android.weave.syncadapter;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.DobbyUtil;
import org.emergent.android.weave.PrefKey;
import org.emergent.android.weave.R;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.util.Dbg;
import org.emergent.android.weave.client.WeaveAccountInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
    implements AdapterView.OnItemSelectedListener, View.OnClickListener
{

  private static final String TAG = Dbg.getTag(AuthenticatorActivity.class);

  public static final String EXTRA_ALLCERTS = "allcerts";
  public static final String EXTRA_AUTHACTION = "authAction";
  public static final String EXTRA_SERVER = Constants.USERDATA_SERVER_KEY; // "server";
  public static final String EXTRA_USERNAME = Constants.USERDATA_USERNAME_KEY; // "username";
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

  private AccountManager m_accountMgr;

  private AsyncTask<WeaveAccountInfo, Integer, AuthResult> mAuthThread;
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

    Button button;

    button = (Button)findViewById(R.id.next_button);
    button.setOnClickListener(this);

    Properties defs = Constants.getRuntimeDefaults();
    if (!defs.isEmpty()) {
      button = (Button)findViewById(R.id.test_button);
      button.setOnClickListener(this);
      button.setVisibility(View.VISIBLE);
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
        initWidgetTestData();
        break;
//      case R.id.custom_server_button:
//        Intent intent = new Intent(this, SpinnerActivity.class);
//        intent.putExtra(EXTRA_BADCERTS, m_badcerts);
//        if (m_server != null)
//          intent.putExtra(EXTRA_SERVER, m_server);
//        startActivityForResult(intent, REQ_CUSTOM_SERVER_CODE);
//        break;
    }
  }

  /**
   * When the user selects an item in the spinner, this method is invoked by the callback chain. Android calls the item
   * selected listener for the spinner, which invokes the onItemSelected method.
   *
   * @param parent - the AdapterView for this listener
   * @param v      - the View for this listener
   * @param pos    - the 0-based position of the selection in the mLocalAdapter
   * @param row    - the 0-based row number of the selection in the View
   * @see AdapterView.OnItemSelectedListener#onItemSelected(AdapterView, View, int, long)
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
    if (mAuthThread != null) {
      mAuthThread.cancel(true);
      mAuthThread = null;
      finish();
    }
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

    mAuthThread = new AsyncTask<WeaveAccountInfo, Integer, AuthResult>() {
      @Override
      protected AuthResult doInBackground(WeaveAccountInfo... accountInfos) {
        WeaveAccountInfo accountInfo = accountInfos[0];
        try {
          NetworkUtilities.authenticate(AuthenticatorActivity.this, accountInfo);
          return new AuthResult(true, accountInfo, null, null);
        } catch (Throwable e) {
          Log.w(TAG, e);
          return new AuthResult(false, accountInfo, "auth failed", e);
        }
      }

      @Override
      protected void onProgressUpdate(Integer... values) {
//          super.onProgressUpdate(values);
      }

      @Override
      protected void onPostExecute(AuthResult result) {
        onAuthenticationResult(result);
      }
    };

    mAuthThread.execute(loginInfo);
  }

  /**
   * Called when the authentication process completes (see attemptLogin()).
   */
  private void onAuthenticationResult(AuthResult authResult) {
    WeaveAccountInfo loginInfo = authResult.m_info;
    Log.d(TAG, String.format("onAuthenticationResult() result = %s ; action = %s", authResult.m_success, m_authAction));
    hideProgress();
    if (authResult.m_success && loginInfo != null) {
      final String serverUri = loginInfo.getServerAsString();
      final String username = loginInfo.getUsername();
      final String password = loginInfo.getPassword();
      final String secret = loginInfo.getSecretAsString();
      final String authToken = NetworkUtilities.createAuthToken(serverUri, username, password, secret);
      switch (m_authAction) {
        case AUTH_ACTION_ADDACCOUNT:
          finishAddAccount(serverUri, username, password, secret, authToken);
          break;
        case AUTH_ACTION_UNKNOWN:
        case AUTH_ACTION_UPDATECREDENTIALS:
          finishUpdateCreds(serverUri, username, password, secret, authToken);
          break;
        case AUTH_ACTION_CONFIRMCREDENTIALS:
          finishConfirmCreds(serverUri, username, password, secret, authToken);
          break;
        default:
      }
    } else {
      Log.e(TAG, "onAuthenticationResult: failed to authenticate");
      updateHintMessage();
    }
  }

  private void finishAddAccount(String serverUri, String username, String password, String secret, String authToken) {
    final Account account = new Account(username, Constants.ACCOUNT_TYPE);
    Bundle userData = new Bundle();
    userData.putString(Constants.USERDATA_SERVER_KEY, serverUri);
    userData.putString(Constants.USERDATA_SECRET_KEY, secret);
    m_accountMgr.addAccountExplicitly(account, password, userData);
    // Set contacts sync for this account.
    ContentResolver.setSyncAutomatically(account, Bookmarks.AUTHORITY, true);
    ContentResolver.setSyncAutomatically(account, Passwords.AUTHORITY, true);

    final Intent intent = new Intent();
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
    if (Constants.AUTHTOKEN_TYPE.equals(m_authtokenType)) {
      intent.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
    }
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);
    finish();
  }

  private void finishUpdateCreds(String serverUri, String username, String password, String secret, String authToken) {
    final Account account = new Account(username, Constants.ACCOUNT_TYPE);
    m_accountMgr.setPassword(account, password);
    m_accountMgr.setUserData(account, Constants.USERDATA_SECRET_KEY, secret);
    m_accountMgr.setUserData(account, Constants.USERDATA_SERVER_KEY, serverUri);
    final Intent intent = new Intent();
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
    if (Constants.AUTHTOKEN_TYPE.equals(m_authtokenType)) {
      intent.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
    }
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);
    finish();
  }

  private void finishConfirmCreds(String serverUri, String username, String password, String secret, String authToken) {
    final Intent intent = new Intent();
    Account account = new Account(username, Constants.ACCOUNT_TYPE);
    m_accountMgr.setPassword(account, password);
    m_accountMgr.setUserData(account, Constants.USERDATA_SERVER_KEY, serverUri);
    m_accountMgr.setUserData(account, Constants.USERDATA_SECRET_KEY, secret);
    intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, true);
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);
    finish();
  }

  /**
   * Hides the progress UI for a lengthy operation.
   */
  protected void hideProgress() {
    dismissDialog(0);
  }

  /**
   * Shows the progress UI for a lengthy operation.
   */
  protected void showProgress() {
    showDialog(0);
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
    Log.w(TAG, String.format("Authenticating: %s : %s @ %s", username, password, serverUri));
    WeaveAccountInfo retval = createWeaveInfo(serverUri, username, password, secret);
    final SharedPreferences prefs = DobbyUtil.getAccountPreferences(AuthenticatorActivity.this, retval);
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
}

