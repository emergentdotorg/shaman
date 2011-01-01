package org.emergent.android.weave.persistence;

import android.net.Uri;

/**
 * @author Patrick Woodworth
 */
public class PasswordContentProvider extends AbstractContentProvider {

  @Override
  protected String getTableName(Uri uri) {
    return PASSWORD_TABLE_NAME;
  }

  @Override
  protected Uri getTableUri(Uri uri) {
    return Passwords.CONTENT_URI;
  }

  @Override
  protected String getTypeSuffix(Uri uri) {
    return "password";
  }
}
