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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;
import org.emergent.android.weave.util.Dbg;
import org.emergent.android.weave.client.QueryResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author Patrick Woodworth
 */
class SyncCache {

  private static final long MGD_MISMATCH = -1;
  private static final long MGD_MISSING = 0;

  private static final String TAG = Dbg.getTag(SyncCache.class);

  protected static final int DATABASE_VERSION = 4;

  private static final String DATABASE_NAME = "synccache.db";

  protected static final String META_GLOBAL_TABLE_NAME = "MgEngDat";

  protected static final String KEY_HASH_TABLE_NAME = "KeyHashDat";

//  private static final AtomicReference<Map<String, String>> sm_bytesMap =
//      new AtomicReference<Map<String, String>>(new ConcurrentHashMap<String, String>());

  private final CacheDbHelper m_helper;

  public SyncCache(Context context) {
    m_helper = new CacheDbHelper(context);
  }

//  public AbstractKeyManager createKeyManager(UserWeave weave, char[] secret) {
////    if (!useCaches)
////      sm_bytesMap.set(new ConcurrentHashMap<String,String>());
//    return new SyncCacheKeyManager(weave, secret, m_helper);
//  }
//
//  private class SyncCacheKeyManager extends AbstractKeyManager {
//
////    private final Map<String, String> m_bytesMap;
//    private final CacheDbHelper m_dbHelper;
//
//    private SyncCacheKeyManager(UserWeave weave, char[] secret, CacheDbHelper dbHelper) {
//      super(weave, secret);
////      m_bytesMap = bytesMap;
//      m_dbHelper = dbHelper;
//    }
//
//    @Override
//    protected String lookup(String key) {
////      return m_bytesMap.get(key);
//
//      String retval = null;
//      SQLiteDatabase db = null;
//      Cursor cur = null;
//      try {
//        db = m_dbHelper.getWritableDatabase();
//        cur = query(db, KEY_HASH_TABLE_NAME,
//            new String[]{KhColumns.KEY_DATA},
//            KhColumns.NODE_URI + " = ?",
//            new String[]{key});
//
//        if (cur.moveToFirst())
//          retval = cur.getString(cur.getColumnIndex(KhColumns.KEY_DATA));
//
//      } finally {
//        if (cur != null) try { cur.close(); } catch (Exception ignored) { }
//        if (db != null) try { db.close(); } catch (Exception ignored) { }
//      }
//      return retval;
//    }
//
//    @Override
//    protected void store(String key, String data) {
////      m_bytesMap.put(key, data);
//
//      SQLiteDatabase db = null;
//      try {
//        db = m_dbHelper.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(KhColumns.NODE_URI, key);
//        values.put(KhColumns.KEY_DATA, data);
//        long rowId = db.insert(KEY_HASH_TABLE_NAME, null, values);
//        Log.d(TAG, "SyncCacheKeyManager.store() : rowid = " + rowId);
//      } finally {
//        if (db != null) try {
//          db.close();
//        } catch (Exception ignored) {
//        }
//      }
//    }
//  }

  public void clear() {
    SQLiteDatabase db = null;
    try {
      db = m_helper.getWritableDatabase();
      int count = db.delete(KEY_HASH_TABLE_NAME, null, null);
      Log.d(TAG, "SyncCacheKeyManager.clear() : count = " + count);
    } finally {
      if (db != null) try {
        db.close();
      } catch (Exception ignored) {
      }
    }
  }

  public void reset() {
    SQLiteDatabase db = null;
    try {
      db = m_helper.getWritableDatabase();
      int count = db.delete(KEY_HASH_TABLE_NAME, null, null);
      count += db.delete(META_GLOBAL_TABLE_NAME, null, null);
      Log.d(TAG, "SyncCacheKeyManager.reset() : count = " + count);
    } finally {
      if (db != null) try {
        db.close();
      } catch (Exception ignored) {
      }
    }
  }

  /**
   * @param queryResult the resulte of querying the meta/global node
   * @return false if caches must be expired
   */
  public Date validateMetaGlobal(QueryResult<JSONObject> queryResult, String engineName) {
    long retval = 0;
    SQLiteDatabase db = null;
    try {
      String uriStr = queryResult.getUri().toASCIIString();
      JSONObject jsonObj = queryResult.getValue();
      Properties flattened = convertMetaGlobalToFlatProperties(jsonObj);
      String gVer = flattened.getProperty("storageVersion");
      String gSyncId = flattened.getProperty("syncID");
      String eVer = flattened.getProperty(engineName + ".version");
      String eSyncId = flattened.getProperty(engineName + ".syncID");
      db = m_helper.getWritableDatabase();
      retval = checkCacheDb(db, uriStr, engineName, gVer, gSyncId, eVer, eSyncId);
      if (retval > 0) {
        return new Date(retval);
      } else if (retval == MGD_MISMATCH) {
        clear();
      }

      ContentValues values = new ContentValues();
      values.put(MgColumns.NODE_URI, uriStr);
      values.put(MgColumns.ENGINE_NAME, engineName);
      values.put(MgColumns.LAST_MODIFIED, 0);
      values.put(MgColumns.GLOBAL_SYNCID, gSyncId);

      Integer gVerInt = null;
      try {
        gVerInt = Integer.valueOf(gVer);
      } catch (NumberFormatException ignored) {
      }
      values.put(MgColumns.GLOBAL_VERSION, gVerInt);

      values.put(MgColumns.ENGINE_SYNCID, eSyncId);

      Integer eVerInt = null;
      try {
        eVerInt = Integer.valueOf(eVer);
      } catch (NumberFormatException ignored) {
      }
      values.put(MgColumns.ENGINE_VERSION, eVerInt);
      long uriId = db.insert(META_GLOBAL_TABLE_NAME, null, values);

      return null;
    } finally {
      if (db != null) try {
        db.close();
      } catch (Exception ignored) {
      }
    }
  }

  public boolean updateLastSync(URI uri, String engineName, Date lastSyncDate) {
    Log.d(TAG, "SyncCache.updateLastSync()");
    if (lastSyncDate == null) {
      Log.w(TAG, "lastSyncDate was null");
      return false;
    }

    long lastSync = lastSyncDate.getTime();
    SQLiteDatabase db = null;
    try {
      String uriStr = uri.toASCIIString();
      db = m_helper.getWritableDatabase();
      ContentValues values = new ContentValues();
      values.put(MgColumns.LAST_MODIFIED, lastSync);
      long updateCount = db.update(META_GLOBAL_TABLE_NAME, values,
          MgColumns.NODE_URI + " = ? AND " + MgColumns.ENGINE_NAME + " = ?",
          new String[]{uriStr, engineName});
      assert updateCount < 2 : "Should not be able to update more than one row by constraints!";
      return updateCount > 0;
    } finally {
      if (db != null) try {
        db.close();
      } catch (Exception ignored) {
      }
    }
  }

  private long checkCacheDb(SQLiteDatabase db,
                            String nodeUri,
                            String engineName,
                            String globalVersion,
                            String globalSyncId,
                            String engineVersion,
                            String engineSyncId)
  {
    Cursor cur = null;
    try {
      db = m_helper.getWritableDatabase();
      cur = query(db, SyncCache.META_GLOBAL_TABLE_NAME,
          new String[]{
              MgColumns.GLOBAL_VERSION,
              MgColumns.GLOBAL_SYNCID,
              MgColumns.ENGINE_VERSION,
              MgColumns.ENGINE_SYNCID,
              MgColumns.LAST_MODIFIED},
          MgColumns.NODE_URI + " = ? AND " + MgColumns.ENGINE_NAME + " = ?",
          new String[]{nodeUri, engineName});


      if (cur.getCount() > 1) {
        Log.w(TAG, String.format("found more than 1 mgd cached for engine %s at %s", engineName, nodeUri));
      }

      if (cur.moveToFirst()) {
        Log.w(TAG, String.format("found mgd cached for engine %s at %s", engineName, nodeUri));
        String cachedVal;

        cachedVal = cur.getString(cur.getColumnIndex(MgColumns.GLOBAL_VERSION));
        if (cachedVal == null || !cachedVal.equals(globalVersion)) {
//          Log.w(TAG, String.format("mismatch 1 where %s != %s", cachedVal, globalVersion));
          return MGD_MISMATCH;
        }

        cachedVal = cur.getString(cur.getColumnIndex(MgColumns.GLOBAL_SYNCID));
        if (cachedVal == null || !cachedVal.equals(globalSyncId)) {
//          Log.w(TAG, String.format("mismatch 2 where %s != %s", cachedVal, globalSyncId));
          return MGD_MISMATCH;
        }

        cachedVal = cur.getString(cur.getColumnIndex(MgColumns.ENGINE_VERSION));
        if (cachedVal == null || !cachedVal.equals(engineVersion)) {
//          Log.w(TAG, String.format("mismatch 3 where %s != %s", cachedVal, engineVersion));
          return MGD_MISMATCH;
        }

        cachedVal = cur.getString(cur.getColumnIndex(MgColumns.ENGINE_SYNCID));
        if (cachedVal == null || !cachedVal.equals(engineSyncId)) {
//          Log.w(TAG, String.format("mismatch 4 where %s != %s", cachedVal, engineSyncId));
          return MGD_MISMATCH;
        }

        return cur.getLong(cur.getColumnIndex(MgColumns.LAST_MODIFIED));
      } else {
        Log.w(TAG, String.format("no mgd cached cached for engine %s at %s", engineName, nodeUri));
      }
    } finally {
      if (cur != null) try {
        cur.close();
      } catch (Exception ignored) {
      }
    }
    return MGD_MISSING;
  }

  private static Cursor query(SQLiteDatabase db,
                              String tableName,
                              String[] projectionin,
                              String whereClause,
                              String[] selectionArgs)
  {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(tableName);
    return qb.query(db, projectionin, whereClause, selectionArgs, null, null, null /* sortOrder */);
  }

  /**
   * In:
   * <p/>
   * <pre>
   * {
   * "id":"global"
   * "modified": 1.28702415227E9
   * "payload":
   * {"syncID":"JnvqPEn(6~",
   * "storageVersion":3,
   * "engines":{"clients":{"version":1,"syncID":"LwjtCQjdsV"},
   *            "bookmarks":{"version":1,"syncID":"ApPN6v8VY4"},
   *            "forms":{"version":1,"syncID":"UKeuhB.aOZ"},
   *            "tabs":{"version":1,"syncID":"G!nU*7H.7j"},
   *            "history":{"version":1,"syncID":"9Tvy_Vlb44"},
   *            "passwords":{"version":1,"syncID":"yfBi2v7Pp)"},
   *            "prefs":{"version":1,"syncID":"*eONx!6GXA"}}}
   * }
   * </pre>
   * <p/>
   * Out:
   * <pre>
   * syncID : ...
   * storageVersion : ...
   * clients.version : ...
   * clients.syncID : ...
   * ...
   * </pre>
   *
   * @param mgObj
   * @return
   */
  private static Properties convertMetaGlobalToFlatProperties(JSONObject mgObj) {
    Properties retval = new Properties();
    try {
//      Log.w(TAG, "mgObj: " + mgObj.toString(2));
      JSONObject payload = new JSONObject(String.valueOf(mgObj.get("payload")));
      transferIfExists(retval, payload, "syncID", null);
      transferIfExists(retval, payload, "storageVersion", null);
      if (payload.has("engines")) {
        JSONObject enginesObj = payload.getJSONObject("engines");
//        Log.w(TAG, "engObj: " + enginesObj.toString(2));
        for (Iterator iter = enginesObj.keys(); iter.hasNext();) {
          String engName = (String)iter.next();
//          Log.w(TAG, "engName: " + engName);
          JSONObject engObj = enginesObj.getJSONObject(engName);
          transferIfExists(retval, engObj, "syncID", engName + ".");
          transferIfExists(retval, engObj, "version", engName + ".");
        }
      }
    } catch (JSONException e) {
      Log.w(TAG, e);
    }
    return retval;
  }

  private static void transferIfExists(Properties props, JSONObject jsObj, String key, String destPrefix) {
    try {
      if (jsObj.has(key))
        props.setProperty(destPrefix == null ? key : destPrefix + key, String.valueOf(jsObj.get(key)));
    } catch (JSONException e) {
      Log.w(TAG, e);
    }
  }

  public static class KhColumns implements BaseColumns {

    // BaseColumn contains _id.
//    java.lang.String _ID = "_id";
//    java.lang.String _COUNT = "_count";

    public static final String NODE_URI = "nodeUri";
    public static final String KEY_DATA = "keyData";
  }


  public static class MgColumns implements BaseColumns {

    // BaseColumn contains _id.
//    java.lang.String _ID = "_id";
//    java.lang.String _COUNT = "_count";

    public static final String NODE_URI = "nodeUri";
    public static final String ENGINE_NAME = "engName";
    public static final String LAST_MODIFIED = "lastModified";
    public static final String GLOBAL_SYNCID = "gSyncId";
    public static final String GLOBAL_VERSION = "gVer";
    public static final String ENGINE_SYNCID = "engSyncId";
    public static final String ENGINE_VERSION = "engVer";
  }

  private static class CacheDbHelper extends SQLiteOpenHelper {

    public CacheDbHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      List<ColumnBuilder> cols;
      cols = new ArrayList<ColumnBuilder>();
      onCreateNodeEntryTable(cols);
      onCreateTable(db, META_GLOBAL_TABLE_NAME, cols);
      cols = new ArrayList<ColumnBuilder>();
      onCreateKeyHashTable(cols);
      onCreateTable(db, KEY_HASH_TABLE_NAME, cols);
    }

    public void onCreateTable(SQLiteDatabase db, String tname, List<ColumnBuilder> cols) {
      StringBuffer buf;
      buf = new StringBuffer();
      buf.append("CREATE TABLE " + tname + "( ");
      int colCnt = 0;
      for (ColumnBuilder col : cols) {
        colCnt++;
        if (colCnt != 1)
          buf.append(", ");
        buf.append(col.m_name).append(" ").append(col.m_type);
      }
      int keyCnt = 0;
      for (ColumnBuilder col : cols) {
        if (!col.m_isPrimaryKey)
          continue;
        keyCnt++;
        if (keyCnt == 1)
          buf.append(", PRIMARY KEY( ");
        else
          buf.append(", ");
        buf.append(col.m_name).append(" ");
      }
      if (keyCnt > 0) {
        buf.append(") ON CONFLICT REPLACE");
      }
      buf.append(");");
      db.execSQL(buf.toString());
    }

    protected void onCreateNodeEntryTable(List<ColumnBuilder> buf) {
//      buf.append(Weaves.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT");
      addColumnToCreate(buf, MgColumns.NODE_URI, "TEXT NOT NULL", true); // UNIQUE ON CONFLICT REPLACE");
      addColumnToCreate(buf, MgColumns.ENGINE_NAME, "TEXT NOT NULL", true);
      addColumnToCreate(buf, MgColumns.LAST_MODIFIED, "NUMERIC NOT NULL DEFAULT (strftime('%s','now'))");
      addColumnToCreate(buf, MgColumns.GLOBAL_SYNCID, "TEXT");
      addColumnToCreate(buf, MgColumns.GLOBAL_VERSION, "INTEGER");
      addColumnToCreate(buf, MgColumns.ENGINE_SYNCID, "TEXT");
      addColumnToCreate(buf, MgColumns.ENGINE_VERSION, "INTEGER");
//      buf.append(", UNIQUE( " + Columns.NODE_URI + ", " + Columns.ENGINE_NAME + ") ON CONFLICT REPLACE");
    }

    protected void onCreateKeyHashTable(List<ColumnBuilder> buf) {
      addColumnToCreate(buf, KhColumns.NODE_URI, "TEXT NOT NULL", true);
      addColumnToCreate(buf, KhColumns.KEY_DATA, "TEXT");
    }

    private void addColumnToCreate(List<ColumnBuilder> buf, String column, String typeSpec) {
      buf.add(new ColumnBuilder(column, typeSpec));
//      buf.append(", ").append(column).append(" ").append(typeSpec);
    }

    private void addColumnToCreate(List<ColumnBuilder> buf, String column, String typeSpec, boolean primaryKey) {
      buf.add(new ColumnBuilder(column, typeSpec, primaryKey));
//      buf.append(", ").append(column).append(" ").append(typeSpec);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + META_GLOBAL_TABLE_NAME);
      onCreate(db);
    }
  }

  public static class ColumnBuilder {

    private String m_name;
    private String m_type;
    private boolean m_isPrimaryKey;

    public ColumnBuilder(String name, String type) {
      this(name, type, false);
    }

    public ColumnBuilder(String name, String type, boolean primaryKey) {
      m_name = name;
      m_type = type;
      m_isPrimaryKey = primaryKey;
    }
  }
}
