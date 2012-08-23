package org.emergent.android.weave.util;

import android.util.Log;

/**
 * @author Patrick Woodworth
 */
public class Logger {

  private final String m_name;
  private static final Logger sm_logger = new Logger(Dbg.LOG_TAG);

  private Logger(String name) {
    m_name = name;
  }

  public static Logger getLogger(Class clazz) {
    return sm_logger;
  }

  public void verbose(String message) {
    Log.v(m_name, message);
  }

  public void error(Throwable e) {
    Log.e(m_name, e.getMessage(), e);
  }

  public void error(String message, Throwable e) {
    Log.e(m_name, message, e);
  }
}
