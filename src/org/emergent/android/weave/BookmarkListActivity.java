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

package org.emergent.android.weave;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.syncadapter.SyncAssistant;
import org.emergent.android.weave.util.Dbg;

/**
 * @author Patrick Woodworth
 */
public class BookmarkListActivity extends AbstractListActivity {

  private static final String TAG = Dbg.getTag(BookmarkListActivity.class);

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bookmark_list);
    setupAdapter();
//    final ListView listView = (ListView)findViewById(R.id.listView);
    final ListView listView = this.getListView();

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> a, View v, int position, long id) {
        Cursor cursor = (Cursor)m_adapter.getItem(position);
        String uri = cursor.getString(cursor.getColumnIndex(Bookmarks.Columns.BMK_URI));
        Log.v(TAG, "launching " + uri);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
      }
    });

    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Context context = view.getContext();
        Log.v(TAG, "context match: " + (BookmarkListActivity.this == context));
        Intent myIntent = new Intent(view.getContext(), BookmarkEditActivity.class);
        myIntent.putExtra(Constants.ROW_ID_INTENT_EXTRA_KEY, id);
        startActivityForResult(myIntent, 0);
        return false;
      }
    });
  }

  @Override
  protected SyncAssistant createSyncAssistant() {
    return new SyncAssistant(this, Bookmarks.UPDATER);
  }

  @Override
  protected SimpleCursorAdapter createCursorAdapter(FilterQueryProvider qfp) {
    String[] from = {Bookmarks.Columns.TITLE};
    int[] to = {R.id.textName};
    Cursor cursor = qfp.runQuery(null);
    return new MyCursorAdapter(this, R.layout.bookmark_row, cursor, from, to);
  }

  @Override
  protected FilterQueryProvider getQueryFilterProvider(final ContentResolver cr) {
    return new FilterQueryProvider() {
//      @Override
      public Cursor runQuery(CharSequence charSequence) {
        Cursor retval;
        String sort = Bookmarks.Columns.TITLE;
        String selection;
        String[] selectionArgs;
        if (TextUtils.isEmpty(charSequence)) {
          selection = "" + Bookmarks.Columns.TYPE + " = ?";
          selectionArgs = new String[]{"bookmark"};
        } else {
          selection = "(" + Bookmarks.Columns.TYPE + " = ?)";
          selection += " AND ((" + Bookmarks.Columns.TITLE + " LIKE ?)";
          selection += " OR (" + Bookmarks.Columns.BMK_URI + " LIKE ?)";
          selection += " OR (" + Bookmarks.Columns.TAGS + " LIKE ?))";
          selectionArgs = new String[]{
              "bookmark",
              "%" + charSequence.toString() + "%",
              "%" + charSequence.toString() + "%",
              "%|" + charSequence.toString() + "|%"
          };
        }
        retval = cr.query(Bookmarks.CONTENT_URI, null, selection, selectionArgs, sort);
        startManagingCursor(retval);
        return retval;
      }
    };
  }
}
