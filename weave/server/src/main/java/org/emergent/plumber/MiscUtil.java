/*
 * Copyright 2011 Patrick Woodworth
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

package org.emergent.plumber;

import javax.servlet.GenericServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;

/**
 * @author Patrick Woodworth
 */
public class MiscUtil {

  public static void dumpRequestParameters(GenericServlet servlet, HttpServletRequest req) {
    for (Enumeration enu = req.getParameterNames(); enu.hasMoreElements();) {
      String parmName = (String)enu.nextElement();
      String parmValue = req.getParameter(parmName);
      servlet.log(String.format("parm : %s = %s", parmName, parmValue));
    }
  }

  public static String toWeaveTimestamp(long millis) {
    return String.format("%.2f", millis / 1000.0);
  }

  public static double toWeaveTimestampDouble(long millis) {
    return (millis / 10) / 100.0;
  }

  public static String readBody(HttpServletRequest req) throws IOException {
    BufferedReader reader = req.getReader();
    StringWriter writer = new StringWriter();
    String line = "";
    while ((line = reader.readLine()) != null) {
      writer.write(line);
//      writer.write("\n");
    }
    return writer.toString();
  }
}
