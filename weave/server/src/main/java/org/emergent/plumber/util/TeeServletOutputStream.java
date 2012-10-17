package org.emergent.plumber.util;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
* @author Patrick Woodworth
*/
public class TeeServletOutputStream extends ServletOutputStream {

  private final ServletOutputStream m_outs[];

  public TeeServletOutputStream(ServletOutputStream[] outs) {
    m_outs = outs;
  }

  @Override
  public void flush() throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.flush();
    }
  }

  @Override
  public void close() throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.close();
    }
  }

  @Override
  public void write(int c) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.write(c);
    }
  }

  @Override
  public void write(byte[] buf, int off, int len) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.write(buf, off, len);
    }
  }

  @Override
  public void write(byte[] buf) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.write(buf);
    }
  }

  @Override
  public void print(boolean b) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.print(b);
    }
  }

  @Override
  public void print(char c) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.print(c);
    }
  }

  @Override
  public void print(int i) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.print(i);
    }
  }

  @Override
  public void print(long l) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.print(l);
    }
  }

  @Override
  public void print(float f) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.print(f);
    }
  }

  @Override
  public void print(double d) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.print(d);
    }
  }

  @Override
  public void print(String s) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.print(s);
    }
  }

  @Override
  public void println() throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println();
    }
  }

  @Override
  public void println(boolean x) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(char x) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(int x) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(long x) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(float x) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(double x) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(String x) throws IOException {
    for (ServletOutputStream o : m_outs) {
      o.println(x);
    }
  }
}
