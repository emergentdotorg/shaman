package org.emergent.plumber;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Patrick Woodworth
 */
public class RedirectServlet extends AbstractBaseServlet {

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    parseUserAndNode(req);
    ServletConfig config = getServletConfig();
    ServletContext context = config.getServletContext();
    String nodeName = (String)req.getAttribute(ATTRIB_SUBPATH_KEY);
    String redirPrefix = getInitParameter(INITPARM_REDIR_PREFIX_KEY);
    String dispatchPath = (redirPrefix != null ? redirPrefix : "") + nodeName;
//    log("disppath: " + dispatchPath);
    RequestDispatcher rd = context.getRequestDispatcher(dispatchPath);
    if (rd != null) {
      rd.forward(req, resp);
    } else {
      super.service(req, resp);
    }
  }
}
