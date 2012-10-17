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
import org.emergent.android.weave.util.Dbg.Log;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.client.CollectionNode;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author Patrick Woodworth
 */
public class Bookmarks implements Constants.Implementable {

  public static final String AUTHORITY = Constants.BOOKMARK_PROVIDER_AUTHORITY;

  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  public static final Weaves.Updater UPDATER =
      new Weaves.Updater(CONTENT_URI, CollectionNode.STORAGE_BOOKMARKS) {
    @Override
    protected void setContentValues(ContentValues values, Weaves.Record info) throws JSONException {
      Bookmarks.setContentValues(values, info);
    }
  };

  private static void setContentValues(ContentValues values, Weaves.Record info) throws JSONException {
    Weaves.setBasicContentValues(values, info);
    boolean isDeleted = info.isDeleted();
    if (!isDeleted) {
      Weaves.putColumnValue(values, Columns.BMK_URI, info.getProperty("bmkUri"));
      Weaves.putColumnValue(values, Columns.TITLE, info.getProperty("title"));
      Weaves.putColumnValue(values, Columns.TYPE, info.getProperty("type"));
      Weaves.putColumnValue(values, Columns.PARENT_ID, info.getProperty("parentid"));
      Weaves.putColumnValue(values, Columns.PREDECESSOR_ID, info.getProperty("predecessorid"));
      String tags = info.getProperty("tags");
      String tagval = null;
      if (tags != null) {
        tagval = "";
        try {
          JSONArray jsonArray = new JSONArray(tags);
          for (int ii = 0; ii < jsonArray.length(); ii++) {
            if (tagval.length() < 1)
              tagval += "|";
            tagval += jsonArray.getString(ii) + "|";
          }
        } catch (Exception ignored) {
        }
      }
      Log.v(TAG, "tagval: " + tagval);
      Weaves.putColumnValue(values, Columns.TAGS, tagval);
    }
  }

  public static final class Columns extends Weaves.Columns {

    public static final String BMK_URI = "bmkUri";
    public static final String TITLE = "title";
    public static final String TYPE = "bType";
    public static final String PARENT_ID = "parentId";
    public static final String PREDECESSOR_ID = "predecessorId";
    public static final String TAGS = "tags";
  }
}
