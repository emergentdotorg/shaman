package org.emergent.plumber.util;

/**
 * @author Patrick Woodworth
 */

import org.emergent.plumber.MiscUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;

public class DumpFilter implements Filter {

  private boolean m_dumpRequest;
  private boolean m_dumpResponse;
  private boolean m_dumpAppend;
  private String m_dumpFile;

  public void init(FilterConfig filterConfig) throws ServletException {
    m_dumpRequest = Boolean.valueOf(filterConfig.getInitParameter("dumpRequest"));
    m_dumpResponse = Boolean.valueOf(filterConfig.getInitParameter("dumpResponse"));
    m_dumpAppend = Boolean.valueOf(filterConfig.getInitParameter("dumpAppend"));
    m_dumpFile = filterConfig.getInitParameter("dumpFile");
  }

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    if (!(servletRequest instanceof HttpServletRequest)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    PrintStream ds = null;
    try {
      ds = getDumpStream();
      HttpServletRequest httpRequest = (HttpServletRequest)servletRequest;
      HttpServletResponse httpResponse = (HttpServletResponse)servletResponse;

      if (m_dumpRequest) {
        ServletUtil.doFilter2(httpRequest, ds);
        MyRequestWrapper bufferedRequest = new MyRequestWrapper(httpRequest);
        httpRequest = bufferedRequest;
        String body = bufferedRequest.getBody();
        ds.println("REQUEST -> " + body);
      }

      if (m_dumpResponse) {
        MyResponseWrapper wrappedResp = new MyResponseWrapper(httpResponse);
        filterChain.doFilter(httpRequest, wrappedResp);
        String body = wrappedResp.getBody();
        ds.println("RESPONSE -> " + body);
      } else {
        filterChain.doFilter(httpRequest, httpResponse);
      }
    } finally {
      if (m_dumpFile != null) {
        MiscUtil.close(ds);
      }
    }
  }

  private PrintStream getDumpStream() throws IOException {
    PrintStream retval = System.out;
    if (m_dumpFile != null) {
      File file = new File(m_dumpFile);
      FileOutputStream fs = new FileOutputStream(file, m_dumpAppend);
      retval = new PrintStream(fs, true);
    }
    return retval;
  }

  public void destroy() {
  }

  private static class MyRequestWrapper extends HttpServletRequestWrapper {

    private byte[] m_buffer;
    private String m_encoding;

    public MyRequestWrapper(HttpServletRequest req) throws IOException {
      super(req);
      m_encoding = req.getCharacterEncoding();
      InputStream is = req.getInputStream();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      MiscUtil.copy(is, baos);
      m_buffer = baos.toByteArray();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(m_buffer);
      return new BufferedServletInputStream(bais);
    }

    @Override
    public BufferedReader getReader() throws IOException {
      InputStream is = getInputStream();
      InputStreamReader isr = m_encoding == null
          ? new InputStreamReader(is)
          : new InputStreamReader(is, m_encoding);
      return new BufferedReader(isr);
    }

    public byte[] getBuffer() {
      return m_buffer;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public String getBody() throws IOException {
      byte[] bytes = getBuffer();
      String enc = m_encoding;
      String retval = enc == null
          ? new String(bytes)
          : new String(bytes, enc);
      return retval;
    }

    private static class BufferedServletInputStream extends ServletInputStream {

      private ByteArrayInputStream m_bais;

      public BufferedServletInputStream(ByteArrayInputStream bais) {
        m_bais = bais;
      }

      public int available() {
        return m_bais.available();
      }

      public int read() {
        return m_bais.read();
      }

      public int read(byte[] buf, int off, int len) {
        return m_bais.read(buf, off, len);
      }
    }
  }

  private static class MyResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream m_baos = new ByteArrayOutputStream();
    private final HttpServletResponse m_wrapped;
    private PrintWriter m_baosPrintWriter = null;
    private ServletOutputStream m_baosServletOutputStream = null;

    public MyResponseWrapper(HttpServletResponse response) {
      super(response);
      m_wrapped = response;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      PrintWriter wrappedWriter = m_wrapped.getWriter();
      m_baosPrintWriter = new PrintWriter(m_baos, true);
      wrappedWriter = new TeePrintWriter(new PrintWriter[]{wrappedWriter, m_baosPrintWriter});
      return wrappedWriter;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      ServletOutputStream wrappedSos = m_wrapped.getOutputStream();
      m_baosServletOutputStream = new ByteArrayServletOutputStream(m_baos);
      wrappedSos = new TeeServletOutputStream(new ServletOutputStream[]{wrappedSos, m_baosServletOutputStream});
      return wrappedSos;
    }

    public byte[] getByteArray() {
      try {
        if (m_baosPrintWriter != null) {
          m_baosPrintWriter.flush();
        }
        if (m_baosServletOutputStream != null) {
          m_baosServletOutputStream.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return m_baos.toByteArray();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public String getBody() throws IOException {
      byte[] bytes = getByteArray();
      String enc = getCharacterEncoding();
      String retval = enc == null
          ? new String(bytes)
          : new String(bytes, enc);
      return retval;
    }
  }

  private static class ByteArrayServletOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream m_baos;

    public ByteArrayServletOutputStream(ByteArrayOutputStream baos) {
      m_baos = baos;
    }

    @Override
    public void write(int b) throws IOException {
      m_baos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      m_baos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      m_baos.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      m_baos.flush();
    }

    @Override
    public void close() throws IOException {
      m_baos.close();
    }
  }
}
