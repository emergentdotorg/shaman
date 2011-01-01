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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.R;
import org.emergent.android.weave.util.Dbg;

/**
 * Service to handle Account authentication. It instantiates the authenticator and returns its IBinder.
 */
public class AuthenticationService extends Service {

  private static final String TAG = Dbg.getTag(AuthenticationService.class);

  private Authenticator m_authenticator;

  @Override
  public void onCreate() {
    // auth service started
    m_authenticator = new Authenticator(this);
  }

  @Override
  public void onDestroy() {
    // auth service stopped
  }

  @Override
  public IBinder onBind(Intent intent) {
    return m_authenticator.getIBinder();
  }

  private static class Authenticator extends AbstractAccountAuthenticator {

    /**
     * Authentication Service context
     */
    private final Context m_context;

    public Authenticator(Context context) {
      super(context);
      m_context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType,
                             String authTokenType,
                             String[] requiredFeatures,
                             Bundle options)
    {
      Log.d(TAG, "Authenticator.addAccount()");
      final Bundle retval = new Bundle();
      Account[] existingAccounts = AccountManager.get(m_context).getAccountsByType(Constants.ACCOUNT_TYPE);
      if (existingAccounts != null && existingAccounts.length > 0) {
        retval.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
        String errMsg = String.format("Only one %s account allowed!", getAuthTokenLabel());
        retval.putString(AccountManager.KEY_ERROR_MESSAGE, errMsg);
      } else {
        final Intent intent = new Intent(m_context, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.EXTRA_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthenticatorActivity.EXTRA_AUTHACTION, AuthenticatorActivity.AUTH_ACTION_ADDACCOUNT);
        retval.putParcelable(AccountManager.KEY_INTENT, intent);
      }
      return retval;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
      Log.d(TAG, "Authenticator.confirmCredentials()");
      final Bundle result = new Bundle();
      if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
        final String password = options.getString(AccountManager.KEY_PASSWORD);
        final Bundle optBundle = options.getBundle(AccountManager.KEY_USERDATA);
        if (optBundle != null) {
          String serverUri = optBundle.getString(Constants.USERDATA_SERVER_KEY);
          String encsecret = optBundle.getString(Constants.USERDATA_SECRET_KEY);
          final String authToken = authenticate(serverUri, account.name, password, encsecret);
          result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, authToken != null);
          return result;
        }
      }

      // Launch AuthenticatorActivity to confirm credentials
      final Intent intent = new Intent(m_context, AuthenticatorActivity.class);
      intent.putExtra(AuthenticatorActivity.EXTRA_USERNAME, account.name);
      intent.putExtra(AuthenticatorActivity.EXTRA_AUTHACTION, AuthenticatorActivity.AUTH_ACTION_CONFIRMCREDENTIALS);
      intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
      result.putParcelable(AccountManager.KEY_INTENT, intent);
      return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account,
                                    String authTokenType,
                                    Bundle options)
    {
      Log.d(TAG, "Authenticator.updateCredentials()");
      final Intent intent = new Intent(m_context, AuthenticatorActivity.class);
      intent.putExtra(AuthenticatorActivity.EXTRA_USERNAME, account.name);
      intent.putExtra(AuthenticatorActivity.EXTRA_AUTHTOKEN_TYPE, authTokenType);
      intent.putExtra(AuthenticatorActivity.EXTRA_AUTHACTION, AuthenticatorActivity.AUTH_ACTION_UPDATECREDENTIALS);
      final Bundle bundle = new Bundle();
      bundle.putParcelable(AccountManager.KEY_INTENT, intent);
      return bundle;
    }
    
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
      Log.d(TAG, "Authenticator.editProperties()");
      throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse resp,
                               Account account,
                               String authTokenType,
                               Bundle options)
    {
      Log.d(TAG, "Authenticator.getAuthToken()");
      final Bundle result = new Bundle();
      if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
        result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
      } else {
        final AccountManager am = AccountManager.get(m_context);
        final String password = am.getPassword(account);
        final String serverUri = am.getUserData(account, Constants.USERDATA_SERVER_KEY);
        final String secret = am.getUserData(account, Constants.USERDATA_SECRET_KEY);
        final String authToken = authenticate(serverUri, account.name, password, secret);
        if (authToken != null) {
          result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
          result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
          result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        } else {
          // Either the server URI, password, or secret was missing, unparseable or incorrect.
          // Return an Intent to an Activity that will prompt the user to correct the problem.
          final Intent intent = new Intent(m_context, AuthenticatorActivity.class);
          intent.putExtra(AuthenticatorActivity.EXTRA_USERNAME, account.name);
          intent.putExtra(AuthenticatorActivity.EXTRA_SERVER, serverUri);
          if (serverUri == null) {
            intent.putExtra(AuthenticatorActivity.EXTRA_HINT_MESSAGE, "The server URI is missing or unparseable.");
            if (password != null) {
              intent.putExtra(AuthenticatorActivity.EXTRA_PASSWORD, password);
              if (secret != null)
                intent.putExtra(AuthenticatorActivity.EXTRA_SECRET, secret);
            }
          } else {
            intent.putExtra(AuthenticatorActivity.EXTRA_HINT_MESSAGE, "The password or secret is incorrect.");
          }
          intent.putExtra(AuthenticatorActivity.EXTRA_AUTHTOKEN_TYPE, authTokenType);
          intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, resp);
          result.putParcelable(AccountManager.KEY_INTENT, intent);
        }
      }
      return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
      if (authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
        return getAuthTokenLabel();
      }
      return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
      Log.d(TAG, "Authenticator.hasFeatures()");
      final Bundle result = new Bundle();
      result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
      return result;
    }

    private String authenticate(String serverUri, String username, String password, String secret) {
      Log.d(TAG, "AuthenticationService$Authenticator.authenticate()");
      try {
        return NetworkUtilities.authenticate(m_context, serverUri, username, password, secret);
      } catch (Exception ignored) {
        Log.w(TAG, ignored);
      }
      return null;
    }

    private String getAuthTokenLabel() {
      return m_context.getString(R.string.account_label);
    }
  }
}
