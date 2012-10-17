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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author Patrick Woodworth
 */
public class AbstractBaseServlet extends HttpServlet {

  public static final String INITPARM_API_VERSION_KEY = "apiVersion";
  public static final String INITPARM_REDIR_PREFIX_KEY = "redirPrefix";

  private static final String ATTRIB_KEY_PREFIX = Config.PLUMBER_PACKAGE_NAME + ".";

  public static final String ATTRIB_USERNAME_KEY = ATTRIB_KEY_PREFIX + "username";
  public static final String ATTRIB_SUBPATH_KEY = ATTRIB_KEY_PREFIX + "subpath";
  public static final String ATTRIB_SUBPARTS_KEY = ATTRIB_KEY_PREFIX + "pathparts";
  public static final String ATTRIB_COLNAME_KEY = ATTRIB_KEY_PREFIX + "colname";
  public static final String ATTRIB_ENTRYID_KEY = ATTRIB_KEY_PREFIX + "entryid";
  public static final String ATTRIB_SRVRTS_KEY = ATTRIB_KEY_PREFIX + "srvrts";
  public static final String WEAVE_TIMESTAMP_HEADER = "X-Weave-Timestamp";

  public Connection getDatabaseConnection() throws SQLException {
    Config config = Config.getInstance(getServletContext());
    return config.getDatabaseConnection();
  }

  public Connection getDatabaseConnection(ServletRequest req) throws SQLException {
    return getDatabaseConnection();
  }

  protected void doSrvrTs(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    long tsmillis = getServerTimestamp(req);
    if (!resp.containsHeader(WEAVE_TIMESTAMP_HEADER)) {
      String weaveTimestamp = String.format("%.2f", tsmillis / 1000.0);
      resp.setHeader(WEAVE_TIMESTAMP_HEADER, weaveTimestamp);
    }
  }

  protected long getServerTimestamp(HttpServletRequest req) throws ServletException, IOException {
    Long tsmillis = (Long)req.getAttribute(ATTRIB_SRVRTS_KEY);
    if (tsmillis == null) {
      tsmillis = System.currentTimeMillis();
      req.setAttribute(ATTRIB_SRVRTS_KEY, tsmillis);
    }
    return tsmillis;
  }

  protected String getServerTimestampString(HttpServletRequest req) throws ServletException, IOException {
    return String.format("%.2f", getServerTimestamp(req) / 1000.0);
  }

  protected void parseUserAndNode(HttpServletRequest req) throws ServletException, IOException {
    if (req.getAttribute(ATTRIB_USERNAME_KEY) != null)
      return;

    String reqUrl = req.getRequestURL().toString();
    String reqUri = req.getRequestURI();
    String ctxPath = req.getContextPath();
    String pathInfo = req.getPathInfo();
    String servletPath = req.getServletPath();
    log("requrl: " + reqUrl);
    log("requri: " + reqUri);
    log("ctxpath: " + ctxPath);
    log("pathinfo: " + pathInfo);
    log("servletpath: " + servletPath);

    if (pathInfo == null)
      return;

    String apiVersion = getInitParameter(INITPARM_API_VERSION_KEY);
    String userName = null;

    StringBuilder nodeNameBuf = new StringBuilder();
    String[] pathComponents = pathInfo.split("/");
    ArrayList<String> partList = new ArrayList<String>();
    for (String pathComponent : pathComponents) {
      if ("".equals(pathComponent)) {
        // ignore empty path components
      } else if (apiVersion == null) {
        apiVersion = pathComponent;
      } else if (userName == null) {
        userName = pathComponent;
      } else {
        partList.add(pathComponent);
        nodeNameBuf.append('/').append(pathComponent);
      }
    }

    String subPath = nodeNameBuf.toString();

    req.setAttribute(ATTRIB_USERNAME_KEY, userName);
    req.setAttribute(ATTRIB_SUBPATH_KEY, subPath);
    req.setAttribute(ATTRIB_SUBPARTS_KEY, partList);

    log("subpath: " + subPath);
    log("username: " + userName);
  }
}
