/*
 * Copyright 2010 Patrick Woodworth
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

package org.emergent.android.weave.syncadapter;

import android.content.Context;
import org.emergent.android.weave.persistence.Passwords;

/**
 * @author Patrick Woodworth
 */
public class PasswordSyncService extends AbstractSyncService {

  private static final Object sm_syncAdapterLock = new Object();
  private static AbstractSyncAdapter sm_syncAdapter = null;

  @Override
  protected AbstractSyncAdapter getSyncAdapter() {
    synchronized (sm_syncAdapterLock) {
      if (sm_syncAdapter == null) {
        sm_syncAdapter = new PasswordSyncAdapter(getApplicationContext(), true);
      }
    }
    return sm_syncAdapter;
  }

  private static class PasswordSyncAdapter extends AbstractSyncAdapter {

    public PasswordSyncAdapter(Context context, boolean autoInitialize) {
      super(context, autoInitialize, Passwords.UPDATER);
    }
  }
}
