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
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.util.Dbg;
import org.emergent.android.weave.client.WeaveException;
import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

/**
 * @author Patrick Woodworth
 */
abstract class AbstractSyncService extends Service {

  private static final String TAG = Dbg.getTag(AbstractSyncService.class);

  @Override
  public IBinder onBind(Intent intent) {
    return getSyncAdapter().getSyncAdapterBinder();
  }

  protected abstract AbstractSyncAdapter getSyncAdapter();

  protected static abstract class AbstractSyncAdapter extends AbstractThreadedSyncAdapter {

    protected final AccountManager m_accountMgr;
    private final SyncAssistant m_helper;

    public AbstractSyncAdapter(Context context, boolean autoInitialize, Weaves.Updater updater) {
      super(context, autoInitialize);
      m_accountMgr = AccountManager.get(context);
      m_helper = new SyncAssistant(context, updater);
    }

    @Override
    public void onPerformSync(Account accnt, Bundle ext, String auth, ContentProviderClient prov, SyncResult res) {
      String authToken = null;
      try {
        authToken = getAuthToken(accnt);
        if (TextUtils.isEmpty(authToken)) {
          res.stats.numAuthExceptions++;
        } else {
          res.stats.numInserts += m_helper.doQueryAndUpdate(authToken);
        }
      } catch (AuthenticatorException e) {
        res.stats.numParseExceptions++;
        Log.e(TAG, "AuthenticatorException", e);
      } catch (OperationCanceledException e) {
        Log.e(TAG, "OperationCanceledExcetpion", e);
      } catch (IOException e) {
        Log.e(TAG, "IOException", e);
        res.stats.numIoExceptions++;
      } catch (AuthenticationException e) {
        if (authToken != null)
          m_accountMgr.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken);
        res.stats.numAuthExceptions++;
        Log.e(TAG, "AuthenticationException", e);
      } catch (ParseException e) {
        res.stats.numParseExceptions++;
        Log.e(TAG, "ParseException", e);
      } catch (final JSONException e) {
        res.stats.numParseExceptions++;
        Log.e(TAG, "JSONException", e);
      } catch (URISyntaxException e) {
        res.stats.numParseExceptions++;
        Log.e(TAG, "URISyntaxException", e);
      } catch (GeneralSecurityException e) {
        if (authToken != null)
          m_accountMgr.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken);
        res.stats.numAuthExceptions++;
        Log.e(TAG, "GeneralSecurityException", e);
      } catch (WeaveException e) {
        Log.e(TAG, "WeaveException", e);
      } catch (Exception e) {
        Log.e(TAG, "Exception", e);
      }
    }

    private String getAuthToken(Account acct) throws IOException, AuthenticatorException, OperationCanceledException {
      return m_accountMgr.blockingGetAuthToken(acct, Constants.AUTHTOKEN_TYPE, true); // todo last parm?
    }
  }

}
