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

import org.emergent.android.weave.util.Dbg.*;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.concurrent.atomic.AtomicReference;

import static org.emergent.android.weave.util.Dbg.*;

public abstract class WeaveListFragment extends ListFragment implements Constants.Implementable {

  protected static final String FILTERSTRING_BUNDLE_KEY = "filterString";

  protected static final String QUERY_KEY = "QUERY_KEY";
  protected static final String CONTENT_URI_KEY = "CONTENT_URI_KEY";
  protected static final String SORT_ORDER_KEY = "SORT_ORDER_KEY";

  protected EditText m_filterEdit = null;

  protected SimpleCursorAdapter m_adapter = null;

  protected final AtomicReference<CharSequence> m_filterString = new AtomicReference<CharSequence>();

  protected TextWatcher m_filterEditWatcher = new FilterTextWatcher();

  private WeaveLoaderCallbacks m_loaderCallbacks;

  private String m_fragTag = null;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    Log.v(TAG, getClass().getSimpleName() + ".onAttach (" + hashCode() + ")");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, getClass().getSimpleName() + ".onCreate (" + hashCode() + ") " + (savedInstanceState != null));
  }

  @Override
  public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v(TAG, getClass().getSimpleName() + ".onCreateView (" + hashCode() + "): " + (savedInstanceState != null));
    return inflater.inflate(R.layout.fragment_location_list, null);
  }

  /**
   * This calls in order:
   * <pre>
   *  onInnerActivityCreated(savedInstanceState);
   *  setupAdapter();
   *  onInnerPostAdapterSetup(savedInstanceState);
   * </pre>
   * @param savedInstanceState
   */
  @Override
  public final void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.v(TAG, getClass().getSimpleName() + ".onActivityCreated (" + hashCode() + "): " + (savedInstanceState != null));
    onInnerActivityCreated(savedInstanceState);
    setupAdapter();
    onInnerPostAdapterSetup(savedInstanceState);
  }

  public void onInnerActivityCreated(Bundle savedInstanceState) {
  }

  protected void setupAdapter() {
    Activity activity = getActivity();
    if (activity != null) {
      m_adapter = createWeaveCursorAdapter();
      setMyListAdapter(m_adapter);
    } else {
      Log.e(TAG, getClass().getSimpleName() + ".setupAdapter (" + hashCode() + "): activity was null!");
    }
  }

  public void onInnerPostAdapterSetup(Bundle savedInstanceState) {
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.v(TAG, getClass().getSimpleName() + ".onStart (" + hashCode() + ")");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.v(TAG, getClass().getSimpleName() + ".onResume (" + hashCode() + ")");
  }

  @Override
  public void onPause() {
    String fragTag = getFragTag();
    Log.v(TAG, getClass().getSimpleName() + ".onPause (" + hashCode() + "): \"" + fragTag + "\"");
    if (fragTag != null) {
      Activity activity = getActivity();
      if (activity instanceof FragUtils.FragmentDataStore) {
        Bundle fragBundle = new Bundle();
        onSaveInstanceState(fragBundle);
        ((FragUtils.FragmentDataStore)activity).setFragData(fragTag, fragBundle);
      }
    }
    super.onPause();
  }

  @Override
  public final void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.v(TAG, getClass().getSimpleName() + ".onSaveInstanceState (" + hashCode() + ")");
    onInnerSaveInstanceState(outState);
  }

  public void onInnerSaveInstanceState(Bundle outState) {
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.v(TAG, getClass().getSimpleName() + ".onStop (" + hashCode() + ")");
  }

  @Override
  public void onDestroyView() {
    Log.v(TAG, getClass().getSimpleName() + ".onDestroyView (" + hashCode() + ")");
    if (m_filterEdit != null) {
      m_filterEdit.removeTextChangedListener(m_filterEditWatcher);
    }
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    Log.v(TAG, getClass().getSimpleName() + ".onDestroy (" + hashCode() + ")");
    if (m_adapter != null) {
      getLoaderManager().destroyLoader(0);
    }
    super.onDestroy();
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.v(TAG, getClass().getSimpleName() + ".onDetach (" + hashCode() + ")");
  }

  public String getFragTag() {
    if (m_fragTag == null) {
      Bundle args = getArguments();
      if (args != null)
        m_fragTag = args.getString(FRAG_TAG_BUNDLE_KEY);
    }
    Log.d(TAG, "WeaveListFragment.getFragTag: \"" + m_fragTag + "\"");
    return m_fragTag;
  }

  protected void validateFilter() {
    Bundle args = createLoaderBundle();
    getLoaderManager().restartLoader(0, args, getLoaderCallbacks());
  }

  public WeaveLoaderCallbacks getLoaderCallbacks() {
    synchronized (this) {
      if (m_loaderCallbacks == null)
        m_loaderCallbacks = createLoaderCallbacks();
    }
    return m_loaderCallbacks;
  }

  public Context getCursorLoaderContext() {
    return getActivity();
  }

  public CursorAdapter getCustomCursorAdapter() {
    return m_adapter;
  }

  public void setMyListAdapter(SimpleCursorAdapter retval) {
    setListAdapter(retval);
    Bundle args = createLoaderBundle();
    getLoaderManager().initLoader(0, args, getLoaderCallbacks());
  }

  protected abstract Bundle createLoaderBundle();

  protected abstract WeaveLoaderCallbacks createLoaderCallbacks();

  protected abstract SimpleCursorAdapter createWeaveCursorAdapter();

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

  public abstract static class WeaveLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

    private final WeaveListFragment m_activity;

    public WeaveLoaderCallbacks(WeaveListFragment activity) {
      m_activity = activity;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      // Swap the new cursor in.  (The framework will take care of closing the old cursor once we return.)
      m_activity.getCustomCursorAdapter().swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
      // This is called when the last Cursor provided to onLoadFinished()
      // above is about to be closed.  We need to make sure we are no
      // longer using it.
      m_activity.getCustomCursorAdapter().swapCursor(null);
    }
  }
}
