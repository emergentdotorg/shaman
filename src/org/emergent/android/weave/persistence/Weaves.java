package org.emergent.android.weave.persistence;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;
import org.emergent.android.weave.client.UserWeave;
import org.emergent.android.weave.client.WeaveBasicObject;
import org.emergent.android.weave.client.WeaveUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public class Weaves {

  public static void setBasicContentValues(ContentValues values, Record info) throws JSONException {
    boolean isDeleted = info.isDeleted();
    putColumnValue(values, Columns.UUID, info.getId());
    putColumnValue(values, Columns.IS_DELETED, isDeleted);
    putColumnValue(values, Columns.LAST_MODIFIED, info.getModifiedInSeconds());
  }

  public static void putColumnValue(ContentValues values, String colName, String value) {
    values.put(colName, value);
  }

  public static void putColumnValue(ContentValues values, String colName, long value) {
    values.put(colName, value);
  }

  public static void putColumnValue(ContentValues values, String colName, boolean value) {
    values.put(colName, value);
  }

  public static class Columns implements BaseColumns {

    // BaseColumn contains _id.

    public static final String UUID = "uuid";
    public static final String SORT_INDEX = "sortIndex";
    public static final String LAST_MODIFIED = "lastModified";
    public static final String IS_DELETED = "isDeleted";
  }

  public static abstract class Updater {

    private final Uri m_authority;
    private final UserWeave.CollectionNode m_collectionNode;

    Updater(Uri authority, UserWeave.CollectionNode colNode) {
      m_authority = authority;
      m_collectionNode = colNode;
    }

    public Uri getAuthority() {
      return m_authority;
    }

    public String getNodePath() {
      return m_collectionNode.nodePath;
    }

    public String getEngineName() {
      return m_collectionNode.engineName;
    }

    public int deleteRecords(ContentResolver cResolver) {
      return cResolver.delete(m_authority, "", new String[] {} );
    }

    public void insertRecord(ContentResolver cResolver, Record info) throws JSONException {
      ContentValues values = new ContentValues();
      setContentValues(values, info);
      cResolver.insert(m_authority, values);
    }

    public int insertRecords(ContentResolver cResolver, List<Record> infos) throws JSONException {
      ContentValues[] valuesArray = new ContentValues[infos.size()];
      int ii = 0;
      for (Record info : infos) {
        ContentValues values = new ContentValues();
        setContentValues(values, info);
        valuesArray[ii++] = values;
      }
      return cResolver.bulkInsert(m_authority, valuesArray);
    }

    protected abstract void setContentValues(ContentValues values, Record info) throws JSONException;
  }

  /**
   * @author Patrick Woodworth
   */
  public static class Record {

    private WeaveBasicObject m_wbo;
    private JSONObject m_decryptedPayload;

    public Record(WeaveBasicObject wbo, JSONObject decryptedPayload) {
      m_wbo = wbo;
      m_decryptedPayload = decryptedPayload;
    }

    public String getId() throws JSONException {
      return getProperty("id");
    }


    public boolean isDeleted() throws JSONException {
      return m_decryptedPayload.has("deleted") && m_decryptedPayload.getBoolean("deleted");
    }

    public String getModified() throws JSONException {
      return m_wbo.getModified();
    }

    public long getModifiedInSeconds() {
      long mod = 0;
      try {
        double modDouble = Double.parseDouble(getModified()) * 1000;
        mod = Math.round(modDouble) / 1000;
      } catch (Exception ignored) {
      }
      return mod;
    }

    public Date getModifiedDate() throws JSONException {
      return WeaveUtil.toModifiedTimeDate(getModified());
    }

    public String getProperty(String key) throws JSONException {
      if (m_decryptedPayload.has(key))
        return m_decryptedPayload.getString(key);
      return null;
    }
  }
}
