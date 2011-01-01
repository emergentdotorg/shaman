package org.emergent.android.weave.persistence;

import android.net.Uri;

/**
 * @author Patrick Woodworth
 */
public class BookmarkContentProvider extends AbstractContentProvider {

  @Override
  protected String getTableName(Uri uri) {
    return BOOKMARK_TABLE_NAME;
  }

  @Override
  protected Uri getTableUri(Uri uri) {
    return Bookmarks.CONTENT_URI;
  }

  @Override
  protected String getTypeSuffix(Uri uri) {
    return "password";
  }
}
