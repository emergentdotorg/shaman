package org.emergent.android.weave;

import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.util.Dbg;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Patrick Woodworth
 */
public abstract class CursorUtil {

  private static final String TAG = Dbg.getTag(CursorUtil.class);
  public static final String QUERY_KEY = "QUERY_KEY";
  public static final String CONTENT_URI_KEY = "CONTENT_URI_KEY";
  public static final String SORT_ORDER_KEY = "SORT_ORDER_KEY";

  private final AbstractListActivity m_activity;

  public CursorUtil(AbstractListActivity activity) {
    m_activity = activity;
  }

  protected AbstractListActivity getActivity() {
    return m_activity;
  }

  public abstract void validateFilter();

  public abstract SimpleCursorAdapter createCursorAdapter();

  public static class CursorUtilViewBinder implements SimpleCursorAdapter.ViewBinder {

    private final SimpleDateFormat m_dateFormat = new SimpleDateFormat();

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
      if (view instanceof TextView &&
          Weaves.Columns.LAST_MODIFIED.equals(cursor.getColumnName(columnIndex))) {
        TextView tview = (TextView)view;
        long theDateLong = cursor.getLong(columnIndex);
        Date theDate = new Date(theDateLong * 1000);
//        GregorianCalendar sm_gregorianCalendar = new GregorianCalendar();
//        sm_gregorianCalendar.setTimeInMillis(theDateLong);
        String dateStr = m_dateFormat.format(theDate);
//        String dateStr = "" + theDate.toString();
//        String dateStr = "" + theDateLong;
        tview.setText(dateStr);
        return true;
      }
      return false;
    }
  }
}
