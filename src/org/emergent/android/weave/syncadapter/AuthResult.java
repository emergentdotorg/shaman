package org.emergent.android.weave.syncadapter;

import org.emergent.android.weave.client.WeaveAccountInfo;

/**
* @author Patrick Woodworth
*/
class AuthResult {

  public final boolean m_success;
  public final WeaveAccountInfo m_info;
  public final String m_message;
  public final Throwable m_thrown;

  public AuthResult(boolean success, WeaveAccountInfo info, String message, Throwable thrown) {
    m_success = success;
    m_info = info;
    m_message = message;
    m_thrown = thrown;
  }
}
