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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.os.Bundle;
import org.emergent.android.weave.Constants;
import org.emergent.android.weave.MainActivity;
import org.emergent.android.weave.R;

/**
* @author Patrick Woodworth
*/
public class FragmentTabListener<T extends Fragment> implements ActionBar.TabListener {

  private final Activity m_activity;
  private final int m_containerViewId;
  private final String m_tag;
  private final Class<T> m_class;
  private final Bundle m_args;

  public FragmentTabListener(Activity activity, int containerViewId, String tag, Class<T> clz, Bundle args) {
    m_activity = activity;
    m_containerViewId = containerViewId;
    m_tag = tag;
    m_class = clz;
    m_args = args;
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
    Fragment frag = getFragment();
    ft.replace(m_containerViewId, frag, m_tag);
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
  }

  private Fragment getFragment() {
    Fragment frag = m_activity.getFragmentManager().findFragmentByTag(m_tag);
    if (frag == null) {
      frag = Fragment.instantiate(m_activity, m_class.getName(), m_args);
    }
    return frag;
  }
}
