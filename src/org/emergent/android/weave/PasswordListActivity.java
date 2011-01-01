package org.emergent.android.weave;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.syncadapter.SyncAssistant;
import org.emergent.android.weave.util.Dbg;

import static org.emergent.android.weave.persistence.Passwords.Columns;


/**
 * @author Patrick Woodworth
 */
public class PasswordListActivity extends AbstractListActivity {

  private static final String TAG = Dbg.getTag(PasswordListActivity.class);

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.password_list);

    setupAdapter();
    // Get the list view
//    final ListView listView = (ListView)findViewById(R.id.listView);
    final ListView listView = this.getListView();


    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor)m_adapter.getItem(position);
        String pword = cursor.getString(cursor.getColumnIndex(Columns.PASSWORD));
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(pword);
        String msg = "Password copied to clipboard.";
        Toast toast = Toast.makeText(PasswordListActivity.this, msg, Toast.LENGTH_SHORT);
        toast.show();
      }
    });

/*
    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Context context = view.getContext();
        Log.v(TAG, "context match: " + (PasswordListActivity.this == context));
        Intent myIntent = new Intent(view.getContext(), PasswordEditActivity.class);
        myIntent.putExtra(Constants.ROW_ID_INTENT_EXTRA_KEY, id);
        startActivityForResult(myIntent, 0);
        return false;
      }
    });
*/

  }

  @Override
  protected SimpleCursorAdapter createCursorAdapter(FilterQueryProvider qfp) {
    String[] from = {Passwords.Columns.HOSTNAME, Passwords.Columns.USERNAME};
    int[] to = {R.id.textName, R.id.textValue};
    Cursor cursor = qfp.runQuery(null);
    return new MyCursorAdapter(this, R.layout.password_row, cursor, from, to);
  }

  @Override
  protected FilterQueryProvider getQueryFilterProvider(final ContentResolver cr) {
    return new FilterQueryProvider() {
//      @Override
      public Cursor runQuery(CharSequence charSequence) {
        Cursor retval;
        String selection;
        String[] selectionArgs;
        if (TextUtils.isEmpty(charSequence)) {
          selection = "" + Weaves.Columns.IS_DELETED + " = ?";
          selectionArgs = new String[]{"0"};
        } else {
          selection = "(" + Weaves.Columns.IS_DELETED + " = ?)";
          selection += " AND ((" + Passwords.Columns.HOSTNAME + " LIKE ?)";
          selection += " OR (" + Passwords.Columns.USERNAME + " LIKE ?))";
          selectionArgs = new String[]{
              "0",
              "%" + charSequence.toString() + "%",
              "%" + charSequence.toString() + "%"
          };
        }
        retval = cr.query(Passwords.CONTENT_URI, null, selection, selectionArgs, Passwords.Columns.HOSTNAME);
        startManagingCursor(retval);
        return retval;
      }
    };
  }

}
