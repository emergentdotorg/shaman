package org.emergent.android.weave;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.util.Dbg;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Patrick Woodworth
 */
public abstract class AbstractListActivity extends ListActivity {

  protected static final boolean USE_LOADER_API = android.os.Build.VERSION.SDK_INT >= 11;

  private static final String TAG = Dbg.getTag(AbstractListActivity.class);

  protected EditText m_filterEdit = null;
  protected SimpleCursorAdapter m_adapter = null;

  protected final AtomicReference<CharSequence> m_filterString = new AtomicReference<CharSequence>();

  protected TextWatcher m_filterEditWatcher = new FilterTextWatcher();

  private Object m_loaderCallbacks;

  protected void validateFilter() {
    if (USE_LOADER_API) {
      LoaderUtil.validateFilter(this);
    } else {
      if (m_adapter != null)
        m_adapter.getFilter().filter(m_filterString.get());
    }
  }

  protected Object getLoaderCallbacks() {
    synchronized (this) {
      if (m_loaderCallbacks == null)
        m_loaderCallbacks = createLoaderCallbacks();
    }
    return m_loaderCallbacks;
  }

  protected abstract Bundle createLoaderBundle();

  protected abstract Object createLoaderCallbacks();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
//    Cursor currentCursor = ((SimpleCursorAdapter)getListAdapter()).getCursor();
//    stopManagingCursor(currentCursor);
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (m_filterEdit != null) {
      m_filterEdit.removeTextChangedListener(m_filterEditWatcher);
    }
    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    SyncManager.handleLoginEditorResult(this, requestCode, resultCode, data);
  }

  protected void setupAdapter() {
    m_filterEdit = (EditText)findViewById(R.id.search_box);
    if (USE_LOADER_API) {
      m_adapter = createCursorAdapter();
    } else {
      ContentResolver cr = getContentResolver();
      FilterQueryProvider qfp = getQueryFilterProvider(cr);
      m_adapter = createCursorAdapter(qfp);
      m_adapter.setFilterQueryProvider(qfp);
      setListAdapter(m_adapter);
    }
    m_filterEdit.addTextChangedListener(m_filterEditWatcher);
  }

  protected abstract SimpleCursorAdapter createCursorAdapter(FilterQueryProvider qfp);

  protected abstract SimpleCursorAdapter createCursorAdapter();

  protected abstract FilterQueryProvider getQueryFilterProvider(ContentResolver cr);

  protected class FilterTextWatcher implements TextWatcher {
    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
      m_filterString.set(s);
      validateFilter();
    }
  }

  public static class MyViewBinder implements SimpleCursorAdapter.ViewBinder {

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

  protected class MyCursorAdapter extends SimpleCursorAdapter {

    public MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
      this(new MyViewBinder(), context, layout, c, from, to);
    }

    public MyCursorAdapter(ViewBinder binder, Context context, int layout, Cursor c, String[] from, int[] to) {
      super(context, layout, c, from, to);
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
     * @see android.widget.ListAdapter#getView(int, View, ViewGroup)
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
