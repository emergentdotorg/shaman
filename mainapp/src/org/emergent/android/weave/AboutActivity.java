package org.emergent.android.weave;


import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import org.emergent.android.weave.util.Dbg.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutActivity extends Activity implements Constants.Implementable {

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.about);
    findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        finish();
      }
    });
    String version = ShamanApplication.getApplicationVersionName();
    if (version != null) {
      TextView text = (TextView)findViewById(R.id.about_version_text_view);
      String vPrefix = getResources().getString(R.string.version);
      text.setText(vPrefix + " " + version);
      text.setVisibility(View.VISIBLE);
    }
    TextView text = (TextView)findViewById(R.id.about_text_view);
    text.setText(readTextFromRawResource(R.raw.license_short));
  }

  public static boolean showAbout(Activity activity) {
    Intent i = new Intent(activity, AboutActivity.class);
    activity.startActivity(i);
    return false;
  }

  private String readTextFromRawResource(int resourceid) {
    String license = "";
    Resources resources = getResources();
    BufferedReader in = new BufferedReader(new InputStreamReader(resources.openRawResource(resourceid)));
    String line;
    StringBuilder sb = new StringBuilder();
    try {
      while ((line = in.readLine()) != null) {
        if (TextUtils.isEmpty(line)) {
          sb.append("\n\n");
        } else {
          sb.append(line);
          sb.append(" ");
        }
      }
      license = sb.toString();
    } catch (IOException e) {
      Log.e(TAG, e.getLocalizedMessage(), e);
    }
    return license;
  }
}
