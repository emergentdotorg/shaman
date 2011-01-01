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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Patrick Woodworth
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class TestDebugFormatter extends Formatter
{
  Date dat = new Date();
  private final static String format = "{0,date} {0,time}";
  private MessageFormat formatter;

  private Object args[] = new Object[1];

  // Line separator string.  This is the value of the line.separator
  // property at the moment that the SimpleFormatter was created.
//    private String lineSeparator = System.getProperty("line.separator");
  private String lineSeparator = (String)java.security.AccessController.doPrivileged(new PrivilegedAction<String>()
  {
    public String run() {
      return System.getProperty("line.separator");
    }
  });

  /**
   * Format the given LogRecord.
   *
   * @param record the log record to be formatted.
   * @return a formatted log record
   */
  public synchronized String format(LogRecord record) {
    StringBuffer sb = new StringBuffer();
    // Minimize memory allocations here.
    dat.setTime(record.getMillis());
    args[0] = dat;
    StringBuffer text = new StringBuffer();
    if (formatter == null) {
      formatter = new MessageFormat(format);
    }
    formatter.format(args, text, null);
    sb.append(text);
    sb.append(" ");
    String sourceClassName = getSourceClassName(record);
    if (sourceClassName != null) {
      sb.append(sourceClassName);
    } else {
      sb.append(record.getLoggerName());
    }
    if (record.getSourceMethodName() != null) {
      sb.append(" ");
      sb.append(record.getSourceMethodName());
    }
    sb.append(" ");
    String message = formatMessage(record);
    sb.append(record.getLevel().getLocalizedName());
    sb.append(": ");
    sb.append(message);
    sb.append(lineSeparator);
    if (record.getThrown() != null) {
      try {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        sb.append(sw.toString());
      } catch (Exception ignored) {
      }
    }
    return sb.toString();
  }

  @SuppressWarnings({"ConstantConditions"})
  private String getSourceClassName(LogRecord record) {
    String retval = record.getSourceClassName();
    int dotIdx = retval == null ? -1 : retval.lastIndexOf('.');
    if (dotIdx > -1) {
      retval = retval.substring(dotIdx + 1);
    }
    return retval;
  }
}
