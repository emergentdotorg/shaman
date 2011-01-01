package org.emergent.android.weave;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import org.emergent.android.weave.persistence.Passwords;

/**
 * @author Patrick Woodworth
 */
public class PasswordEditActivity extends AbstractEditActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.password_edit);
    Intent myIntent = getIntent();
    Bundle extras = myIntent.getExtras();
    long rowid = extras.getLong(Constants.ROW_ID_INTENT_EXTRA_KEY, -1);
//    TextView textView = (TextView)findViewById(R.id.hostnameEdit);
//    textView.setText("rowid: " + rowid);
    Button next = (Button)findViewById(R.id.Button02);
    next.setOnClickListener(new View.OnClickListener() {
//      @Override
      public void onClick(View view) {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
      }
    });
    assignFields(rowid);
  }

  private void assignFields(long rowid) {
    Cursor cursor = createCursorAtRow(Passwords.CONTENT_URI, rowid);
    EditText hostnameEdit = (EditText)findViewById(R.id.hostnameEdit);
    EditText usernameEdit = (EditText)findViewById(R.id.usernameEdit);
    hostnameEdit.setText(getColumnValue(cursor, Passwords.Columns.HOSTNAME));
    usernameEdit.setText(getColumnValue(cursor, Passwords.Columns.USERNAME));
    TableLayout tlayout = (TableLayout)findViewById(R.id.passwordTable);
    EditText httpRealmEdit = addAnotherRow(tlayout, "httpRealm", cursor, Passwords.Columns.HTTP_REALM);
    EditText formSubmitEdit = addAnotherRow(tlayout, "formSubmitUrl", cursor, Passwords.Columns.FORM_SUBMIT_URL);
    addAnotherRow(tlayout, "usernameField", cursor, Passwords.Columns.USERNAME_FIELD);
    addAnotherRow(tlayout, "passwordField", cursor, Passwords.Columns.PASSWORD_FIELD);
    addAnotherRow(tlayout, "password", cursor, Passwords.Columns.PASSWORD);
  }
}
