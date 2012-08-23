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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * @author Patrick Woodworth
 */
public class FragUtils {

  private FragUtils() {
  }

  public static void popFragmentViewDelayed(FragmentManager fm) {
    popFragmentView(fm, false);
  }

  public static void popFragmentViewImmediate(FragmentManager fm) {
    popFragmentView(fm, true);
  }

  private static void popFragmentView(FragmentManager fm, boolean immediate) {
    if (fm == null)
      return;
    int flags = FragmentManager.POP_BACK_STACK_INCLUSIVE;
    if (immediate) {
      fm.popBackStackImmediate(null, flags);
    } else {
      fm.popBackStack(null, flags);
    }
  }

  public static void gotoFragmentView(FragmentManager fm, Fragment fragment) {
    gotoFragmentView(fm, fragment, true);
  }

  public static void gotoFragmentView(FragmentManager fm, Fragment fragment, boolean addBack) {
    if (fm == null)
      return;

    FragmentTransaction ft = fm.beginTransaction();
    if (fragment instanceof CrumbTitled) {
      String title = ((CrumbTitled)fragment).getCrumbTitle();
      if (title != null) {
        title = title.trim();
        final int STITLE_MAX_LEN = 15;
        if (title.length() > STITLE_MAX_LEN) {
          title = title.substring(0, STITLE_MAX_LEN - 3) + "...";
        }
      }
      ft.setBreadCrumbTitle(title);
    }
    ft.replace(R.id.fragment_content, fragment);
    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
    if (addBack) {
      ft.addToBackStack(null);
    }
    ft.commit();
  }


  public static interface BackPressedHandler {
    /**
     * @return true if handled, false if caller should propagate.
     */
    public boolean handleBackPressed();
  }

  static interface FragmentDataStore {

    public Bundle getFragData(String tag);

    public void setFragData(String tag, Bundle bundle);
  }

  public static interface CrumbTitled {

    String getCrumbTitle();
  }
}
