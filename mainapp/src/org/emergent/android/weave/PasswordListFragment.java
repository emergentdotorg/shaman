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

import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.TextUtils;
import org.emergent.android.weave.util.Dbg.Log;
import android.view.View;
import android.widget.*;
import org.emergent.android.weave.persistence.Passwords;
import org.emergent.android.weave.persistence.Weaves;
import org.emergent.android.weave.util.Dbg;

/**
* @author Patrick Woodworth
*/
public class PasswordListFragment extends WeaveListFragment implements FragUtils.CrumbTitled {

  @Override
  public void onInnerActivityCreated(Bundle savedInstanceState) {
    super.onInnerActivityCreated(savedInstanceState);
    m_filterEdit = (EditText)getActivity().findViewById(R.id.search_box);
    m_filterEdit.setVisibility(View.VISIBLE);
  }

  @Override
  public void onInnerPostAdapterSetup(Bundle savedInstanceState) {
    super.onInnerPostAdapterSetup(savedInstanceState);

    m_filterEdit.addTextChangedListener(m_filterEditWatcher);

    Bundle stateBundle = savedInstanceState;
    if (stateBundle == null) {
      Activity activity = getActivity();
      if (activity instanceof FragUtils.FragmentDataStore) {
        stateBundle = ((FragUtils.FragmentDataStore)activity).getFragData(getFragTag());
      }
    }

    if (stateBundle == null) {
      return;
    }

    String filterString = stateBundle.getString(FILTERSTRING_BUNDLE_KEY);
    if (filterString != null)
      m_filterEdit.setText(filterString, TextView.BufferType.NORMAL);
  }

  @Override
  public void onInnerSaveInstanceState(Bundle outState) {
    super.onInnerSaveInstanceState(outState);
    CharSequence filterCharSeq = m_filterString.get();
    outState.putString(FILTERSTRING_BUNDLE_KEY, filterCharSeq == null ? "" : filterCharSeq.toString());
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Activity activity = getActivity();
    if (activity == null) {
      Log.w(TAG, "onListItemClick: Activity was null!");
      return;
    }

    ListAdapter listAdapter = getListAdapter();

    Cursor cursor = (Cursor)listAdapter.getItem(position);
    String pword = cursor.getString(cursor.getColumnIndex(Passwords.Columns.PASSWORD));

    ClipboardManager clipboard = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
    clipboard.setText(pword);
    String msg = "Password copied to clipboard.";
    Toast toast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
    toast.show();
  }

  @Override
  public String getCrumbTitle() {
    return "Passwords";
  }

  @Override
  protected Bundle createLoaderBundle() {
    Bundle args = new Bundle();
    args.putCharSequence(QUERY_KEY, m_filterString.get());
    args.putParcelable(CONTENT_URI_KEY, Passwords.CONTENT_URI);
    args.putString(SORT_ORDER_KEY, Passwords.Columns.HOSTNAME);
    return args;
  }

  @Override
  protected WeaveLoaderCallbacks createLoaderCallbacks() {
    return new PasswordLoaderCallbacks(this);
  }

  @Override
  protected SimpleCursorAdapter createWeaveCursorAdapter() {
    Context activity = getCursorLoaderContext();
    String[] from = {Passwords.Columns.HOSTNAME, Passwords.Columns.USERNAME};
    int[] to = {R.id.textName, R.id.textValue};
    return new WeaveListCursorAdapter(activity, R.layout.password_row, from, to);
  }

  public static class PasswordLoaderCallbacks extends WeaveLoaderCallbacks {

    private final PasswordListFragment m_activity;

    public PasswordLoaderCallbacks(PasswordListFragment activity) {
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
      return new CursorLoader(m_activity.getCursorLoaderContext(), contentUri, null, selection, selectionArgs, sortOrder);
    }
  }
}
