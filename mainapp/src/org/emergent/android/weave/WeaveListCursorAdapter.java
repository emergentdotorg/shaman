/*
 * Copyright 2012 Patrick Woodworth
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

package org.emergent.android.weave;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import org.emergent.android.weave.persistence.Weaves;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
* @author Patrick Woodworth
*/
public class WeaveListCursorAdapter extends SimpleCursorAdapter {

  public WeaveListCursorAdapter(Context context, int layout, String[] from, int[] to) {
    this(new MyViewBinder(), context, layout, null, from, to);
  }

  public WeaveListCursorAdapter(ViewBinder binder, Context context, int layout, Cursor c, String[] from, int[] to) {
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
  @Override
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

  public static class MyViewBinder implements ViewBinder {

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
