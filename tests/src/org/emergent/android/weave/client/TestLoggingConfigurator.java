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

package org.emergent.android.weave.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;

/**
 * @author Patrick Woodworth
 */
public class TestLoggingConfigurator
{
  private static final AtomicBoolean sm_syncObject = new AtomicBoolean();

  public TestLoggingConfigurator() {
    initLogging(true);
  }

  public static void initLogging() {
    initLogging(false);
  }

  private static void initLogging(boolean fromConstructor) {
    if (!(System.getProperty("java.util.logging.config.file") == null))
      return;
    String classProp = System.getProperty("java.util.logging.config.class");
    if (fromConstructor && !TestLoggingConfigurator.class.getName().equals(classProp))
      return;
    if (sm_syncObject.compareAndSet(false, true)) {
      InputStream is = null;
      try {
        is = TestLoggingConfigurator.class.getResourceAsStream("logging.properties");
        if (is != null)
          LogManager.getLogManager().readConfiguration(is);
      } catch (IOException ignored) {
      } finally {
        if (is != null) try { is.close(); } catch (Exception ignored) { }
      }
    }
  }
}
