package org.emergent.plumber.util;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * @author Patrick Woodworth
 */
public class TeeFilter implements Filter {

  public final static String LOGBACK_STATUS_MANAGER_KEY = "LOGBACK_STATUS_MANAGER";
  public static final String LB_INPUT_BUFFER = "LB_INPUT_BUFFER";
  public static final String LB_OUTPUT_BUFFER = "LB_OUTPUT_BUFFER";
  public final static String X_WWW_FORM_URLECODED = "application/x-www-form-urlencoded";
  public final static String IMAGE_CONTENT_TYPE = "image/";
  public final static String IMAGE_JPEG = "image/jpeg";
  public final static String IMAGE_GIF = "image/gif";
  public final static String IMAGE_PNG = "image/png";

  public void init(FilterConfig filterConfig) throws ServletException {
    // NOP
  }

  public void destroy() {
    // NOP
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      try {
        TeeHttpServletRequest teeRequest = new TeeHttpServletRequest((HttpServletRequest)request);
        TeeHttpServletResponse teeResponse = new TeeHttpServletResponse((HttpServletResponse)response);

        //System.out.println("BEFORE TeeFilter. filterChain.doFilter()");
        filterChain.doFilter(teeRequest, teeResponse);
        //System.out.println("AFTER TeeFilter. filterChain.doFilter()");

        teeResponse.finish();
        // let the output contents be available for later use by
        // logback-access-logging
        teeRequest.setAttribute(LB_OUTPUT_BUFFER, teeResponse.getOutputBuffer());
      } catch (IOException e) {
        e.printStackTrace();
        throw e;
      } catch (ServletException e) {
        e.printStackTrace();
        throw e;
      }
    } else {
      filterChain.doFilter(request, response);
    }

  }

  private static boolean isFormUrlEncoded(HttpServletRequest request) {
    if ("POST".equals(request.getMethod()) && X_WWW_FORM_URLECODED.equals(request.getContentType())) {
      return true;
    } else {
      return false;
    }
  }

  private static boolean isImageResponse(HttpServletResponse response) {
    String responseType = response.getContentType();
    if (responseType != null && responseType.startsWith(IMAGE_CONTENT_TYPE)) {
      return true;
    } else {
      return false;
    }
  }

  private static class TeeHttpServletRequest extends HttpServletRequestWrapper {

    private TeeServletInputStream m_inStream;
    private BufferedReader m_reader;
    private boolean m_postedParametersMode = false;

    public TeeHttpServletRequest(HttpServletRequest request) {
      super(request);
      if (isFormUrlEncoded(request)) {
        m_postedParametersMode = true;
      } else {
        m_inStream = new TeeServletInputStream(request);
        // add the contents of the input buffer as an attribute of the request
        request.setAttribute(LB_INPUT_BUFFER, m_inStream.getInputBuffer());
        m_reader = new BufferedReader(new InputStreamReader(m_inStream));
      }
    }

    byte[] getInputBuffer() {
      if (m_postedParametersMode) {
        throw new IllegalStateException("Call disallowed in postedParametersMode");
      }
      return m_inStream.getInputBuffer();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (!m_postedParametersMode) {
        return m_inStream;
      } else {
        return super.getInputStream();
      }
    }

    @Override
    public BufferedReader getReader() throws IOException {
      if (!m_postedParametersMode) {
        return m_reader;
      } else {
        return super.getReader();
      }
    }

    public boolean isPostedParametersMode() {
      return m_postedParametersMode;
    }
  }

  private static class TeeHttpServletResponse extends HttpServletResponseWrapper {

    private TeeServletOutputStream m_teeServletOutputStream;
    private PrintWriter m_teeWriter;

    public TeeHttpServletResponse(HttpServletResponse httpServletResponse) {
      super(httpServletResponse);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      if (m_teeServletOutputStream == null) {
        m_teeServletOutputStream = new TeeServletOutputStream(this.getResponse());
      }
      return m_teeServletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      if (m_teeWriter == null) {
        m_teeWriter = new PrintWriter(new OutputStreamWriter(getOutputStream()), true);
      }
      return m_teeWriter;
    }

    @Override
    public void flushBuffer() {
      if (m_teeWriter != null) {
        m_teeWriter.flush();
      }
    }

    byte[] getOutputBuffer() {
      // m_teeServletOutputStream can be null if the getOutputStream method is never called.
      if (m_teeServletOutputStream != null) {
        return m_teeServletOutputStream.getOutputStreamAsByteArray();
      } else {
        return null;
      }
    }

    void finish() throws IOException {
      if (m_teeWriter != null) {
        m_teeWriter.close();
      }
      if (m_teeServletOutputStream != null) {
        m_teeServletOutputStream.close();
      }
    }
  }

  private static class TeeServletInputStream extends ServletInputStream {

    private InputStream m_in;
    private byte[] m_inputBuffer;

    public TeeServletInputStream(HttpServletRequest request) {
      duplicateInputStream(request);
    }

    @Override
    public int read() throws IOException {
      //System.out.println("zzzzzzzzzz TeeServletInputStream.read called");
      return m_in.read();
    }

    private void duplicateInputStream(HttpServletRequest request) {
      try {
        int len = request.getContentLength();
        ServletInputStream originalSIS = request.getInputStream();
        if (len < 0) {
          m_in = originalSIS;
        } else {
          m_inputBuffer = new byte[len];
          int n = originalSIS.read(m_inputBuffer, 0, len);
          assert n == len;
          m_in = new ByteArrayInputStream(m_inputBuffer);
          originalSIS.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    byte[] getInputBuffer() {
      return m_inputBuffer;
    }
  }

  private static class TeeServletOutputStream extends ServletOutputStream {

    private final ServletOutputStream m_underlyingStream;
    private final ByteArrayOutputStream m_baosCopy;

    public TeeServletOutputStream(ServletResponse httpServletResponse) throws IOException {
      // System.out.println("TeeServletOutputStream.constructor() called");
      m_underlyingStream = httpServletResponse.getOutputStream();
      m_baosCopy = new ByteArrayOutputStream();
    }

    byte[] getOutputStreamAsByteArray() {
      return m_baosCopy.toByteArray();
    }

    @Override
    public void write(int val) throws IOException {
      if (m_underlyingStream != null) {
        m_underlyingStream.write(val);
        m_baosCopy.write(val);
      }
    }

    @Override
    public void write(byte[] byteArray) throws IOException {
      if (m_underlyingStream == null) {
        return;
      }
      // System.out.println("WRITE TeeServletOutputStream.write(byte[]) called");
      write(byteArray, 0, byteArray.length);
    }

    @Override
    public void write(byte byteArray[], int offset, int length) throws IOException {
      if (m_underlyingStream == null) {
        return;
      }
      // System.out.println("WRITE TeeServletOutputStream.write(byte[], int, int)
      // called");
      // System.out.println(new String(byteArray, offset, length));
      m_underlyingStream.write(byteArray, offset, length);
      m_baosCopy.write(byteArray, offset, length);
    }

    @Override
    public void close() throws IOException {
      // System.out.println("CLOSE TeeServletOutputStream.close() called");

      // If the servlet accessing the stream is using a writer instead of
      // an OutputStream, it will probably call os.close() before calling
      // writer.close. Thus, the underlying output stream will be called
      // before the data sent to the writer could be flushed.
    }


    @Override
    public void flush() throws IOException {
      if (m_underlyingStream == null) {
        return;
      }
      // System.out.println("FLUSH TeeServletOutputStream.flush() called");
      m_underlyingStream.flush();
      m_baosCopy.flush();
    }
  }
}
