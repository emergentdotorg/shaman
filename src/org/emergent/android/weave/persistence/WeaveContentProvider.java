package org.emergent.android.weave.persistence;

import android.content.UriMatcher;
import android.database.SQLException;
import android.net.Uri;

/**
 * @author Patrick Woodworth
 */
public class WeaveContentProvider extends AbstractContentProvider {

  protected static final int PASSWORD_URI_ID = 2;
  protected static final int BOOKMARK_URI_ID = 3;

  protected static final UriMatcher sUriMatcher;

  static {
    sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
//    sUriMatcher.addURI(AUTHORITY, "password/*", PASSWORD_URI_ID);
//    sUriMatcher.addURI(AUTHORITY, "bookmark/*", BOOKMARK_URI_ID);
    sUriMatcher.addURI(Bookmarks.AUTHORITY, "", BOOKMARK_URI_ID);
    sUriMatcher.addURI(Passwords.AUTHORITY, "", PASSWORD_URI_ID);
    sUriMatcher.addURI(Bookmarks.AUTHORITY, "*", BOOKMARK_URI_ID);
    sUriMatcher.addURI(Passwords.AUTHORITY, "*", PASSWORD_URI_ID);
    sUriMatcher.addURI("content://" + Bookmarks.AUTHORITY, "", BOOKMARK_URI_ID);
    sUriMatcher.addURI("content://" + Passwords.AUTHORITY, "", PASSWORD_URI_ID);
    sUriMatcher.addURI("content://" + Bookmarks.AUTHORITY, "*", BOOKMARK_URI_ID);
    sUriMatcher.addURI("content://" + Passwords.AUTHORITY, "*", PASSWORD_URI_ID);
  }

  protected int getUriId(Uri uri) {
    int retval = sUriMatcher.match(uri);
    if (retval != -1)
      return retval;
    if ((Bookmarks.AUTHORITY).equals(uri.toString())) {
      return BOOKMARK_URI_ID;
    } else if (("content://" + Bookmarks.AUTHORITY).equals(uri.toString())) {
      return BOOKMARK_URI_ID;
    } else if (Passwords.AUTHORITY.equals(uri.toString())) {
      return PASSWORD_URI_ID;
    } else if (("content://" + Passwords.AUTHORITY).equals(uri.toString())) {
      return PASSWORD_URI_ID;
    }
    return retval;
  }

  @Override
  protected String getTableName(Uri uri) {
    switch (getUriId(uri)) {
      case PASSWORD_URI_ID:
        return PASSWORD_TABLE_NAME;
      case BOOKMARK_URI_ID:
        return BOOKMARK_TABLE_NAME;
    }
    throw new SQLException("Could not determine tablename for " + uri);
  }

  @Override
  protected Uri getTableUri(Uri uri) {
    switch (getUriId(uri)) {
      case PASSWORD_URI_ID:
        return Passwords.CONTENT_URI;
      case BOOKMARK_URI_ID:
        return Bookmarks.CONTENT_URI;
    }
    throw new SQLException("Could not determine tableuri for " + uri);
  }

  @Override
  protected String getTypeSuffix(Uri uri) {
    String suffix = "unknown";
    switch (getUriId(uri)) {
      case PASSWORD_URI_ID:
        suffix = "password";
        break;
      case BOOKMARK_URI_ID:
        suffix = "bookmark";
        break;
      default:
    }
    return suffix;
  }
}
