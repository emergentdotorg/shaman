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
package org.emergent.android.weave.syncadapter;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import org.emergent.android.weave.client.*;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.util.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
* @author Patrick Woodworth
*/
public class SyncAssistant {

  private static final String TAG = Dbg.getTag(SyncAssistant.class);

  private final Context m_context;
  private final Weaves.Updater m_updater;
  private final SyncCache m_syncCache;

  public SyncAssistant(Context context, Weaves.Updater updater) {
    m_context = context;
    m_updater = updater;
    m_syncCache = new SyncCache(m_context);
  }

  public void reset() {
    m_syncCache.reset();
  }

  public int doQueryAndUpdate(String authToken) throws Exception {
    WeaveAccountInfo loginInfo = NetworkUtilities.createWeaveAccountInfo(authToken);
    UserWeave userWeave = NetworkUtilities.createUserWeave(loginInfo, m_context);
    QueryResult<JSONObject> metaGlobal = userWeave.getNode(UserWeave.HashNode.META_GLOBAL);
    ContentResolver resolver = m_context.getContentResolver();
    Date lastSyncDate = m_syncCache.validateMetaGlobal(metaGlobal, m_updater.getEngineName());

    boolean expireCache = (lastSyncDate == null);

    if (expireCache) {
      Log.w(TAG, "expiring caches for " + m_updater.getEngineName());
      m_updater.deleteRecords(resolver);
//        m_syncCache.clear();
    }

    QueryParams parms = new QueryParams();
    if (lastSyncDate != null) {
      Date lastModDate = getLastModified(userWeave, m_updater.getEngineName());
      Log.w(TAG, String.format("compmod %s to %s", lastSyncDate, lastModDate));
      if (lastModDate != null) {
//          Log.w(TAG, String.format("compmod %s to %s", lastSyncDate.getTime(), lastModDate.getTime()));
        if (!lastModDate.after(lastSyncDate))
          return 0;
      }
      parms.setNewer(lastSyncDate);
    }
    boolean useCaches = !expireCache;
//      AbstractKeyManager session = m_syncCache.createKeyManager(userWeave, loginInfo.getSecret());
    QueryResult<List<WeaveBasicObject>> queryResult =
        getCollection(userWeave, m_updater.getNodePath(), parms);
    List<WeaveBasicObject> wboList = queryResult.getValue();
    List<Weaves.Record> records = new ArrayList<Weaves.Record>();
    for (WeaveBasicObject wbo : wboList) {
      JSONObject decryptedPayload = wbo.getEncryptedPayload(userWeave, loginInfo.getSecret());
      records.add(new Weaves.Record(wbo, decryptedPayload));
    }
    int insertCnt = m_updater.insertRecords(resolver, records);
    m_syncCache.updateLastSync(metaGlobal.getUri(), m_updater.getEngineName(), queryResult.getServerTimestamp());
    return insertCnt;
  }

  private Date getLastModified(UserWeave userWeave, String name) throws WeaveException {
    try {
      JSONObject infoCol = userWeave.getNode(UserWeave.HashNode.INFO_COLLECTIONS).getValue();
        Log.w(TAG, "infoCol (" + name + ") : " + infoCol.toString(2));
      if (infoCol.has(name)) {
        long modLong = infoCol.getLong(name);
//        Log.w(TAG, "modLong (" + name + ") : " + modLong);
        return new Date(modLong * 1000);
//          double lastMod = infoCol.getDouble(name);
//          return WeaveUtil.toModifiedTimeDate(lastMod);
      }
      return null;
    } catch (JSONException e) {
      throw new WeaveException(e);
    }
  }

  private QueryResult<List<WeaveBasicObject>> getCollection(UserWeave weave, String name, QueryParams params)
      throws WeaveException
  {
    if (params == null)
      params = new QueryParams();
    URI uri = weave.buildSyncUriFromSubpath(name + params.toQueryString());
    return weave.getWboCollection(uri);
  }
}
