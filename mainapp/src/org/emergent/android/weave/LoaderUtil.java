package org.emergent.android.weave;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.util.Dbg;

/**
 * @author Patrick Woodworth
 */
@TargetApi(11)
public class LoaderUtil {

  public static final String QUERY_KEY = "QUERY_KEY";
  public static final String CONTENT_URI_KEY = "CONTENT_URI_KEY";
  public static final String SORT_ORDER_KEY = "SORT_ORDER_KEY";

  private static final String TAG = Dbg.getTag(LoaderUtil.class);

  public static void validateFilter(AbstractListActivity activity) {
    Bundle args = activity.createLoaderBundle();
    activity.getLoaderManager().restartLoader(0, args, (MyLoaderCallbacks)activity.getLoaderCallbacks());
  }

  public static SimpleCursorAdapter createBookmarkCursorAdapter(BookmarkListActivity activity) {
    String[] from = {Bookmarks.Columns.TITLE, Bookmarks.Columns.BMK_URI};
    int[] to = {R.id.title, R.id.url};
    SimpleCursorAdapter retval = new MyNewCursorAdapter(activity, R.layout.history_item, from, to);
    activity.setListAdapter(retval);
    Bundle args = activity.createLoaderBundle();
    activity.getLoaderManager().initLoader(0, args, (MyLoaderCallbacks)activity.getLoaderCallbacks());
    return retval;
  }

  public static SimpleCursorAdapter createPasswordCursorAdapter(PasswordListActivity activity) {
    String[] from = {Passwords.Columns.HOSTNAME, Passwords.Columns.USERNAME};
    int[] to = {R.id.textName, R.id.textValue};
    SimpleCursorAdapter retval = new MyNewCursorAdapter(activity, R.layout.password_row, from, to);
    activity.setListAdapter(retval);
    Bundle args = activity.createLoaderBundle();
    activity.getLoaderManager().initLoader(0, args, (MyLoaderCallbacks)activity.getLoaderCallbacks());
    return retval;
  }

  public static Object createBookmarkLoaderCallbacks(BookmarkListActivity activity) {
    return new LoaderUtil.BookmarkLoaderCallbacks(activity);
  }

  public static Object createPasswordLoaderCallbacks(PasswordListActivity activity) {
    return new LoaderUtil.PasswordLoaderCallbacks(activity);
  }

  public abstract static class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

    private final AbstractListActivity m_activity;

    public MyLoaderCallbacks(AbstractListActivity activity) {
      m_activity = activity;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      // Swap the new cursor in.  (The framework will take care of closing the old cursor once we return.)
      m_activity.m_adapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
      // This is called when the last Cursor provided to onLoadFinished()
      // above is about to be closed.  We need to make sure we are no
      // longer using it.
      m_activity.m_adapter.swapCursor(null);
    }
  }

  public static class BookmarkLoaderCallbacks extends MyLoaderCallbacks {

    private final BookmarkListActivity m_activity;

    public BookmarkLoaderCallbacks(BookmarkListActivity activity) {
      super(activity);
      m_activity = activity;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      if (args == null) {
        Log.w(TAG,"onCreateLoader: args were null!");
      }
      CharSequence charSequence = args.getCharSequence(QUERY_KEY);
      Uri contentUri = args.getParcelable(CONTENT_URI_KEY);
      String sortOrder = args.getString(SORT_ORDER_KEY);
      String selection;
      String[] selectionArgs;
      if (TextUtils.isEmpty(charSequence)) {
  //          selection = "" + Bookmarks.Columns.TYPE + " = ?";
  //          selectionArgs = new String[]{"bookmark"};
        selection = "" + Bookmarks.Columns.IS_DELETED + " = ?";
        selection += " AND " + Bookmarks.Columns.TYPE + " != ?";
        selection += " AND " + Bookmarks.Columns.TYPE + " != ?";
        selection += " AND " + Bookmarks.Columns.TYPE + " != ?";
        selection += " AND " + Bookmarks.Columns.PARENT_ID + " = ?";
        String parentId = null;
        synchronized (m_activity.m_parentStack) {
          parentId = m_activity.m_parentStack.peek();
        }
        if (parentId == null)
          parentId = "places";
        selectionArgs = new String[]{"0", "livemark", "query", "separator", parentId};
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
      return new CursorLoader(m_activity, contentUri, null, selection, selectionArgs, sortOrder);
    }
  }

  public static class PasswordLoaderCallbacks extends MyLoaderCallbacks {

    private final PasswordListActivity m_activity;

    public PasswordLoaderCallbacks(PasswordListActivity activity) {
      super(activity);
      m_activity = activity;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      CharSequence charSequence = args.getCharSequence(QUERY_KEY);
      Uri contentUri = args.getParcelable(CONTENT_URI_KEY);
      String sortOrder = args.getString(SORT_ORDER_KEY);
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
      return new CursorLoader(m_activity, contentUri, null, selection, selectionArgs, sortOrder);
    }
  }

  public static class MyNewCursorAdapter extends SimpleCursorAdapter {

    public MyNewCursorAdapter(Context context, int layout, String[] from, int[] to) {
      this(new AbstractListActivity.MyViewBinder(), context, layout, null, from, to);
    }

    public MyNewCursorAdapter(ViewBinder binder, Context context, int layout, Cursor c, String[] from, int[] to) {
      super(context, layout, c, from, to, 0);
      this.setViewBinder(binder);
    }

    @Override
    public int getStringConversionColumn() {
      return super.getStringConversionColumn();
    }

    @Override
    public void setStringConversionColumn(int stringConversionColumn) {
      super.setStringConversionColumn(stringConversionColumn);
    }

    @Override
    public Object getItem(int position) {
      return super.getItem(position);
    }

    /**
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = super.getView(position, convertView, parent);
      TextView urlTextView = (TextView)v.findViewById(R.id.url);
      if (urlTextView != null) {
        String urlVal = urlTextView.getText().toString();
        if (urlVal == null || urlVal.trim().length() < 1) {
          urlTextView.setVisibility(View.GONE);
        } else {
          urlTextView.setVisibility(View.VISIBLE);
        }
      }
      View star = v.findViewById(R.id.star);
      if (star != null)
        star.setVisibility(View.GONE);
      View favicon = v.findViewById(R.id.favicon);
      if (favicon != null)
        favicon.setVisibility(View.GONE);
      return v;
    }
  }
}
