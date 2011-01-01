package org.emergent.android.weave;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * @author Patrick Woodworth
 */
public class AbstractEditActivity extends Activity {

  protected String getColumnValue(Cursor cursor, String colName) {
    int colId = cursor.getColumnIndex(colName);
    return cursor.getString(colId);
  }

  protected Cursor createCursorAtRow(Uri providerUri, long rowid) {
    Uri myUri = ContentUris.withAppendedId(providerUri, rowid);
    Cursor cur = managedQuery(myUri, null, null, null, null);
    cur.moveToFirst();
    return cur;
  }

  protected EditText addAnotherRow(TableLayout tlayout, String label) {
    TableRow trow = new TableRow(tlayout.getContext());
    TextView tview = new TextView(trow.getContext());
    tview.setText(label);
    trow.addView(tview);
    EditText etext = new EditText(trow.getContext());
//    etext.setTextAppearance(etext.getContext(), android.R.attr.textAppearanceSmall);
//    etext.setTextScaleX((float)0.8);
    etext.setTextSize(etext.getTextSize() * (float)0.8);
//    etext.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    etext.setSingleLine(true);
    etext.setHorizontallyScrolling(true);
    trow.addView(etext);
    tlayout.addView(trow);
    return etext;
  }

  protected EditText addAnotherRow(TableLayout tlayout, String label, Cursor cursor, String colName) {
    EditText editText = addAnotherRow(tlayout, label);
    editText.setText(getColumnValue(cursor, colName));
    return editText;
  }
}
