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

package org.emergent.android.weave.persistence;

import android.content.ContentValues;
import android.net.Uri;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.client.CollectionNode;
import org.json.JSONException;

/**
 * @author Patrick Woodworth
 */
public class Passwords {

  public static final String AUTHORITY = Constants.PASSWORD_PROVIDER_AUTHORITY;

  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  public static final Weaves.Updater UPDATER =
      new Weaves.Updater(CONTENT_URI, CollectionNode.STORAGE_PASSWORDS) {
    @Override
    protected void setContentValues(ContentValues values, Weaves.Record info) throws JSONException {
      Passwords.setContentValues(values, info);
    }
  };

  private static void setContentValues(ContentValues values, Weaves.Record info) throws JSONException {
    Weaves.setBasicContentValues(values, info);
    boolean isDeleted = info.isDeleted();
    if (!isDeleted) {
      Weaves.putColumnValue(values, Columns.HOSTNAME, info.getProperty("hostname"));
      Weaves.putColumnValue(values, Columns.USERNAME, info.getProperty("username"));
      Weaves.putColumnValue(values, Columns.PASSWORD, info.getProperty("password"));
      Weaves.putColumnValue(values, Columns.FORM_SUBMIT_URL, info.getProperty("formSubmitURL"));
      Weaves.putColumnValue(values, Columns.HTTP_REALM, info.getProperty("httpRealm"));
      Weaves.putColumnValue(values, Columns.USERNAME_FIELD, info.getProperty("usernameField"));
      Weaves.putColumnValue(values, Columns.PASSWORD_FIELD, info.getProperty("passwordField"));
    }
  }

  public static final class Columns extends Weaves.Columns {

    public static final String USERNAME = "username";
    public static final String HOSTNAME = "hostname";
    public static final String PASSWORD = "password";
    public static final String FORM_SUBMIT_URL = "formSubmitURL";
    public static final String HTTP_REALM = "httpRealm";
    public static final String USERNAME_FIELD = "usernameField";
    public static final String PASSWORD_FIELD = "passwordField";
  }
}
