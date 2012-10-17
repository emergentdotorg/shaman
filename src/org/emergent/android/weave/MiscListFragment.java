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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public class MiscListFragment extends ListFragment implements Constants.Implementable {


  public MiscListFragment() {
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Activity activity = getActivity();

    Resources res = getResources();

    List<LocationModel> locationModels = new ArrayList<LocationModel>();
    locationModels.add(new LocationModel(0, "Bookmarks", BookmarkListFragment.class, res.getString(R.string.bookmarks)));
    locationModels.add(new LocationModel(1, "Passwords", PasswordListFragment.class, res.getString(R.string.passwords)));

    if (activity != null) {
      ListAdapter listAdapter = new LocationModelListAdapter(activity,
          locationModels.toArray(new LocationModel[locationModels.size()]));
      setListAdapter(listAdapter);
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Activity activity = getActivity();
    if (activity != null) {
      ListAdapter listAdapter = getListAdapter();
      LocationModel locationModel = (LocationModel) listAdapter.getItem(position);
      MainActivity frontActivity = (MainActivity)activity;
      frontActivity.setMyFragment(locationModel.getFragment(frontActivity));
    }
  }

  public static class LocationModelListAdapter extends BaseAdapter {

    private class ViewHolder {

      public TextView textView;
    }

    private LocationModel[] mLocations;
    private LayoutInflater mInflater;

    public LocationModelListAdapter(Context context, LocationModel[] locations) {
      mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      mLocations = locations;
    }

    @Override
    public int getCount() {
      if (mLocations != null) {
        return mLocations.length;
      }

      return 0;
    }

    @Override
    public Object getItem(int position) {
      if (mLocations != null && position >= 0 && position < getCount()) {
        return mLocations[position];
      }

      return null;
    }

    @Override
    public long getItemId(int position) {
      if (mLocations != null && position >= 0 && position < getCount()) {
        return mLocations[position].m_id;
      }

      return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;
      ViewHolder viewHolder;

      if (view == null) {
        view = mInflater.inflate(R.layout.item_location_list, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.textView = (TextView)view.findViewById(R.id.list_label);

        view.setTag(viewHolder);
      } else {
        viewHolder = (ViewHolder)view.getTag();
      }

      LocationModel locationModel = mLocations[position];

      viewHolder.textView.setText(locationModel.m_address);

      return view;
    }
  }

  public static class LocationModel {

    public int m_id;
    public String m_address;
    private final Class<? extends Fragment> m_class;
    private final String m_tag;
    private final Bundle m_args;

    public LocationModel(int id, String address, Class<? extends Fragment> aClass, String tag) {
      m_id = id;
      m_address = address;
      m_class = aClass;
      m_tag = tag;
      m_args = null;
    }

    private Fragment getFragment(FragmentActivity m_activity) {
      Fragment frag = m_activity.getSupportFragmentManager().findFragmentByTag(m_tag);
      if (frag == null) {
        frag = Fragment.instantiate(m_activity, m_class.getName(), m_args);
      }
      return frag;
    }
  }
}
