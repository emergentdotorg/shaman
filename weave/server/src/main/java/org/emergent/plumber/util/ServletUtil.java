package org.emergent.plumber.util;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Locale;

/**
 * @author Patrick Woodworth
 */
public class ServletUtil {

  public static void doFilter2(ServletRequest request, PrintStream writer) throws IOException, ServletException {
    writer.println("Request Received at " + (new Timestamp(System.currentTimeMillis())));
    writer.println(" characterEncoding=" + request.getCharacterEncoding());
    writer.println("     contentLength=" + request.getContentLength());
    writer.println("       contentType=" + request.getContentType());
    writer.println("            locale=" + request.getLocale());
    writer.print("           locales=");
    Enumeration locales = request.getLocales();
    boolean first = true;
    while (locales.hasMoreElements()) {
      Locale locale = (Locale) locales.nextElement();
      if (first)
        first = false;
      else
        writer.print(", ");
      writer.print(locale.toString());
    }
    writer.println();
    Enumeration names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      writer.print("         parameter=" + name + "=");
      String values[] = request.getParameterValues(name);
      for (int i = 0; i < values.length; i++) {
        if (i > 0)
          writer.print(", ");
        writer.print(values[i]);
      }
      writer.println();
    }
    writer.println("          protocol=" + request.getProtocol());
    writer.println("        remoteAddr=" + request.getRemoteAddr());
    writer.println("        remoteHost=" + request.getRemoteHost());
    writer.println("            scheme=" + request.getScheme());
    writer.println("        serverName=" + request.getServerName());
    writer.println("        serverPort=" + request.getServerPort());
    writer.println("          isSecure=" + request.isSecure());

    // Render the HTTP servlet request properties
    if (request instanceof HttpServletRequest) {
      writer.println("---------------------------------------------");
      HttpServletRequest hrequest = (HttpServletRequest) request;
      writer.println("       contextPath=" + hrequest.getContextPath());
      Cookie cookies[] = hrequest.getCookies();
      if (cookies == null)
        cookies = new Cookie[0];
      for (int i = 0; i < cookies.length; i++) {
        writer.println("            cookie=" + cookies[i].getName() + "=" + cookies[i].getValue());
      }
      names = hrequest.getHeaderNames();
      while (names.hasMoreElements()) {
        String name = (String) names.nextElement();
        String value = hrequest.getHeader(name);
        writer.println("            header=" + name + "=" + value);
      }
      writer.println("            method=" + hrequest.getMethod());
      writer.println("          pathInfo=" + hrequest.getPathInfo());
      writer.println("       queryString=" + hrequest.getQueryString());
      writer.println("        remoteUser=" + hrequest.getRemoteUser());
      writer.println("requestedSessionId=" + hrequest.getRequestedSessionId());
      writer.println("        requestURI=" + hrequest.getRequestURI());
      writer.println("       servletPath=" + hrequest.getServletPath());
    }
    writer.println("=============================================");

    // Log the resulting string
    writer.flush();
//    filterConfig.getServletContext().log(sw.getBuffer().toString());
//
////    Pass control on to the next filter
//    chain.doFilter(request, response);

  }
}
