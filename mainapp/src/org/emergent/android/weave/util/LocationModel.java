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

package org.emergent.android.weave.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.emergent.android.weave.R;

public class LocationModel {

  public int id;
  public String address;

  public LocationModel(int id, String address) {
    this.id = id;
    this.address = address;
  }

  public static class ExampleData {

    // This static field acts as the app's "fake" database of location data.
    public static final LocationModel[] sLocations = {
        new LocationModel(0, "222 W. Washington Ave."),
        new LocationModel(1, "150 E. Gilman St"),
        new LocationModel(2, "114 State St"),
        new LocationModel(3, "23 N. Pinckney St")
    };

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
        return mLocations[position].id;
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

      viewHolder.textView.setText(locationModel.address);

      return view;
    }

  }
}
