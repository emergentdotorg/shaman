package org.emergent.android.weave;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.Toast;
import org.emergent.android.weave.client.WeaveConstants;
import org.emergent.android.weave.persistence.Bookmarks;

/**
 * @author Patrick Woodworth
 */
public class BookmarkEditActivity extends AbstractEditActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bookmark_edit);
    Intent myIntent = getIntent();
    Bundle extras = myIntent.getExtras();
    long rowid = extras.getLong(Constants.ROW_ID_INTENT_EXTRA_KEY, -1);
//    TextView textView = (TextView)findViewById(R.id.hostnameEdit);
//    textView.setText("rowid: " + rowid);
    Button next = (Button)findViewById(R.id.Button02);
    next.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
      }
    });
    assignFields(rowid);
  }

  private void assignFields(long rowid) {
    Cursor cursor = createCursorAtRow(Bookmarks.CONTENT_URI, rowid);
    EditText hostnameEdit = (EditText)findViewById(R.id.titleEdit);
    EditText usernameEdit = (EditText)findViewById(R.id.uriEdit);
    hostnameEdit.setText(getColumnValue(cursor, Bookmarks.Columns.TITLE));
    usernameEdit.setText(getColumnValue(cursor, Bookmarks.Columns.BMK_URI));
    TableLayout tlayout = (TableLayout)findViewById(R.id.bookmarkTable);
  }

}
