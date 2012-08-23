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

import org.emergent.android.weave.client.WeaveUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public abstract class AbstractSyncServlet extends AbstractBaseServlet {

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doSrvrTs(req, resp);
    setReqAttribs(req, resp);
    String username = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    String colname = (String)req.getAttribute(ATTRIB_COLNAME_KEY);
    if (WeaveUtil.isEmpty(username) || WeaveUtil.isEmpty(colname)) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    } else {
      super.service(req, resp);
    }
  }

  protected void setReqAttribs(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String colName = null;
    String entryId = null;
    @SuppressWarnings({"unchecked"}) List<String> partsList = (List<String>)req.getAttribute(ATTRIB_SUBPARTS_KEY);
    if (partsList != null && partsList.size() > 1) {
      colName = partsList.get(1);
      if (partsList.size() > 2)
        entryId = partsList.get(2);
    }

    log("colname: " + colName);
    log("entryid: " + entryId);

    if (colName != null)
      req.setAttribute(ATTRIB_COLNAME_KEY, colName);
    if (entryId != null)
      req.setAttribute(ATTRIB_ENTRYID_KEY, entryId);
  }

}
