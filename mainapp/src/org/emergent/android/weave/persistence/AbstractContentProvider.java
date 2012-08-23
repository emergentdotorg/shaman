package org.emergent.android.weave.persistence;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import org.emergent.android.weave.util.Dbg.Log;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.util.Dbg;

/**
 * @author Patrick Woodworth
 */
abstract class AbstractContentProvider extends ContentProvider implements Constants.Implementable {

  protected static final int DATABASE_VERSION = 5;

  private static final String DATABASE_NAME = "weave.db";

  protected static final String BOOKMARK_TABLE_NAME = "Bookmark";

  protected static final String PASSWORD_TABLE_NAME = "Password";

  protected SQLiteDatabase sqlDB;

  protected SQLiteOpenHelper dbHelper;

  private static final int ROOT_URI_ID = 1;
  private static final int PASSWORD_URI_ID = 2;
  private static final int BOOKMARK_URI_ID = 3;

  private static final UriMatcher sUriMatcher;

  static {
//    sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    sUriMatcher = new UriMatcher(ROOT_URI_ID);
    sUriMatcher.addURI(Passwords.AUTHORITY, null, PASSWORD_URI_ID);
    sUriMatcher.addURI(Bookmarks.AUTHORITY, null, BOOKMARK_URI_ID);
  }

  public AbstractContentProvider() {
  }

  @Override
  public boolean onCreate() {
    Context context = getContext();
    dbHelper = createDatabaseHelper(context);
    return (dbHelper != null);
  }

  private int getUriId(Uri uri) {
    int retval = sUriMatcher.match(uri);
    Log.d(TAG, String.format("getUriId (%s) : %d", uri.toString(), retval));
    if (retval != -1)
      return retval;
//    if ((Bookmarks.AUTHORITY).equals(uri.toString())) {
//      return BOOKMARK_URI_ID;
//    } else if (("content://" + Bookmarks.AUTHORITY).equals(uri.toString())) {
//      return BOOKMARK_URI_ID;
//    } else if (Passwords.AUTHORITY.equals(uri.toString())) {
//      return PASSWORD_URI_ID;
//    } else if (("content://" + Passwords.AUTHORITY).equals(uri.toString())) {
//      return PASSWORD_URI_ID;
//    }
    return retval;
  }

  @Override
  public Uri insert(Uri uri, ContentValues contentvalues) {
//    Log.d(TAG, "insert: uri = " + uri);
//    getUriId(uri);
    // get database to insert records
    sqlDB = dbHelper.getWritableDatabase();
    // insert record in user table and get the row number of recently inserted record
    long rowId = sqlDB.insert(getTableName(uri), "", contentvalues);
    if (rowId > 0) {
      Uri rowUri = ContentUris.appendId(getTableUri(uri).buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(rowUri, null); //todo should we use the last param?
      return rowUri;
    }
    throw new SQLException("Failed to insert row into " + uri);
  }

  @Override
  public int delete(Uri uri, String whereClause, String[] whereArgs) {
    Log.d(TAG, "delete: uri = " + uri);
    sqlDB = dbHelper.getWritableDatabase();
    int count = sqlDB.delete(getTableName(uri), whereClause, whereArgs);
    if (count > 0) {
      getContext().getContentResolver().notifyChange(getTableUri(uri), null); //todo should we use the last param?
    }
    return count;
  }

  @Override
  public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
    Log.d(TAG, "update: uri = " + uri);
    return 0;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    Log.d(TAG, "query: uri = \"" + uri + "\" ; selection = " + selection );
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(getTableName(uri));
    try {
      long rowId = ContentUris.parseId(uri);
      if (rowId > -1) {
        qb.appendWhere(Passwords.Columns._ID + " = " + rowId);
      }
    } catch (NumberFormatException ignored) {
    }
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  @Override
  public String getType(Uri uri) {
    Log.d(TAG, "getType: uri = " + uri);
    String suffix = getTypeSuffix(uri);
    return getTypePrefix(uri) + suffix;
  }

  protected String getTypePrefix(Uri uri) {
    boolean singleResult = false;
    try {
      long rowId = ContentUris.parseId(uri);
      if (rowId > -1) {
        singleResult = true;
      }
    } catch (NumberFormatException ignored) {
    }

    String retval = null;
    if (singleResult) {
      retval = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.emergentdotorg.";
    } else {
      retval = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.emergentdotorg.";
    }
    return retval;
  }

  protected SQLiteOpenHelper createDatabaseHelper(Context context) {
    return new MyDatabaseHelper(context);
  }

  protected abstract String getTypeSuffix(Uri uri);

  protected abstract String getTableName(Uri uri);

  protected abstract Uri getTableUri(Uri uri);

  protected abstract static class BasicSQLiteOpenHelper extends SQLiteOpenHelper {

    public BasicSQLiteOpenHelper(Context context,
                               String name,
                               SQLiteDatabase.CursorFactory factory,
                               int version) {
      super(context, name, factory, version);
    }

    protected void onCreateBasic(StringBuffer buf) {
      buf.append(Weaves.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT");
      buf.append(", " + Weaves.Columns.UUID + " TEXT NOT NULL UNIQUE ON CONFLICT REPLACE");
      buf.append(", " + Weaves.Columns.LAST_MODIFIED + " NUMERIC NOT NULL DEFAULT (strftime('%s','now'))");
      buf.append(", " + Weaves.Columns.IS_DELETED + " NUMERIC");
    }
  }

  private static class MyDatabaseHelper extends BasicSQLiteOpenHelper {

    public MyDatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      StringBuffer buf;
      buf = new StringBuffer();
      buf.append("Create table " + BOOKMARK_TABLE_NAME + "( ");
      onCreateBookmarkTable(buf);
      buf.append(");");
      db.execSQL(buf.toString());

      buf = new StringBuffer();
      buf.append("Create table " + PASSWORD_TABLE_NAME + "( ");
      onCreatePasswordTable(buf);
      buf.append(");");
      db.execSQL(buf.toString());
    }

    protected void onCreateBookmarkTable(StringBuffer buf) {
      super.onCreateBasic(buf);
      buf.append(", " + Bookmarks.Columns.TITLE + " TEXT");
      buf.append(", " + Bookmarks.Columns.BMK_URI + " TEXT");
      buf.append(", " + Bookmarks.Columns.TYPE + " TEXT");
      buf.append(", " + Bookmarks.Columns.PARENT_ID + " TEXT");
      buf.append(", " + Bookmarks.Columns.PREDECESSOR_ID + " TEXT");
      buf.append(", " + Bookmarks.Columns.TAGS + " TEXT");
    }

    protected void onCreatePasswordTable(StringBuffer buf) {
      super.onCreateBasic(buf);
      buf.append(", " + Passwords.Columns.HOSTNAME + " TEXT");
      buf.append(", " + Passwords.Columns.USERNAME + " TEXT");
      buf.append(", " + Passwords.Columns.PASSWORD + " TEXT");
      buf.append(", " + Passwords.Columns.FORM_SUBMIT_URL + " TEXT");
      buf.append(", " + Passwords.Columns.HTTP_REALM + " TEXT");
      buf.append(", " + Passwords.Columns.USERNAME_FIELD + " TEXT");
      buf.append(", " + Passwords.Columns.PASSWORD_FIELD + " TEXT");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + BOOKMARK_TABLE_NAME);
      db.execSQL("DROP TABLE IF EXISTS " + PASSWORD_TABLE_NAME);
      onCreate(db);
    }
  }

}
