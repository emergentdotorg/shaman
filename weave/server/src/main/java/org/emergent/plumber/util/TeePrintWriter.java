package org.emergent.plumber.util;

import java.io.PrintWriter;
import java.util.Locale;

/**
* @author Patrick Woodworth
*/
public class TeePrintWriter extends PrintWriter {

  private final PrintWriter m_outs[];

  public TeePrintWriter(PrintWriter[] outs) {
    super(outs[0]);
    m_outs = outs;
  }

  @Override
  public void flush() {
    for (PrintWriter o : m_outs) {
      o.flush();
    }
  }

  @Override
  public void close() {
    for (PrintWriter o : m_outs) {
      o.close();
    }
  }

  @Override
  public boolean checkError() {
    boolean retval = false;
    for (PrintWriter o : m_outs) {
      retval = o.checkError() || retval;
    }
    return retval;
  }

  @Override
  public void write(int c) {
    for (PrintWriter o : m_outs) {
      o.write(c);
    }
  }

  @Override
  public void write(char[] buf, int off, int len) {
    for (PrintWriter o : m_outs) {
      o.write(buf, off, len);
    }
  }

  @Override
  public void write(char[] buf) {
    for (PrintWriter o : m_outs) {
      o.write(buf);
    }
  }

  @Override
  public void write(String s, int off, int len) {
    for (PrintWriter o : m_outs) {
      o.write(s, off, len);
    }
  }

  @Override
  public void write(String s) {
    for (PrintWriter o : m_outs) {
      o.write(s);
    }
  }

  @Override
  public void print(boolean b) {
    for (PrintWriter o : m_outs) {
      o.print(b);
    }
  }

  @Override
  public void print(char c) {
    for (PrintWriter o : m_outs) {
      o.print(c);
    }
  }

  @Override
  public void print(int i) {
    for (PrintWriter o : m_outs) {
      o.print(i);
    }
  }

  @Override
  public void print(long l) {
    for (PrintWriter o : m_outs) {
      o.print(l);
    }
  }

  @Override
  public void print(float f) {
    for (PrintWriter o : m_outs) {
      o.print(f);
    }
  }

  @Override
  public void print(double d) {
    for (PrintWriter o : m_outs) {
      o.print(d);
    }
  }

  @Override
  public void print(char[] s) {
    for (PrintWriter o : m_outs) {
      o.print(s);
    }
  }

  @Override
  public void print(String s) {
    for (PrintWriter o : m_outs) {
      o.print(s);
    }
  }

  @Override
  public void print(Object obj) {
    for (PrintWriter o : m_outs) {
      o.print(obj);
    }
  }

  @Override
  public void println() {
    for (PrintWriter o : m_outs) {
      o.println();
    }
  }

  @Override
  public void println(boolean x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(char x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(int x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(long x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(float x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(double x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(char[] x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(String x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public void println(Object x) {
    for (PrintWriter o : m_outs) {
      o.println(x);
    }
  }

  @Override
  public PrintWriter printf(String format, Object... args) {
    for (PrintWriter o : m_outs) {
      o.printf(format, args);
    }
    return this;
  }

  @Override
  public PrintWriter printf(Locale l, String format, Object... args) {
    for (PrintWriter o : m_outs) {
      o.printf(l, format, args);
    }
    return this;
  }

  @Override
  public PrintWriter format(String format, Object... args) {
    for (PrintWriter o : m_outs) {
      o.format(format, args);
    }
    return this;
  }

  @Override
  public PrintWriter format(Locale l, String format, Object... args) {
    for (PrintWriter o : m_outs) {
      o.format(l, format, args);
    }
    return this;
  }

  @Override
  public PrintWriter append(CharSequence csq) {
    for (PrintWriter o : m_outs) {
      o.append(csq);
    }
    return this;
  }

  @Override
  public PrintWriter append(CharSequence csq, int start, int end) {
    for (PrintWriter o : m_outs) {
      o.append(csq, start, end);
    }
    return this;
  }

  @Override
  public PrintWriter append(char c) {
    for (PrintWriter o : m_outs) {
      o.append(c);
    }
    return this;
  }
}
