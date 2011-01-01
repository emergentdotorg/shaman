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

import java.net.URI;

/**
 * @author Patrick Woodworth
 */
public class TestUtil {

  static {
    TestLoggingConfigurator.initLogging();
  }

//  public static final String TEST_SERVER = System.getenv("WEAVE_TEST_SERVERURL");
//  public static final String TEST_USERNAME = System.getenv("WEAVE_TEST_USERNAME");
//  public static final String TEST_PASSWORD = System.getenv("WEAVE_TEST_PASSWORD");
//  public static final String TEST_SYNC_KEY = System.getenv("WEAVE_TEST_SYNCKEY");
  
  public static final String TEST_SERVER = "https://auth.services.mozilla.com/";
  public static final String TEST_USERNAME = "weavetest@emergent.org";
  public static final String TEST_PASSWORD = "foobarbang";
  public static final String TEST_SYNC_KEY = "xrwjwg42i6q9ds6ctwhudb9x24";
  
  public static final char[] TEST_SECRET = TEST_SYNC_KEY.toCharArray();

  
  public static WeaveAccountInfo getTestLoginInfo() {
    String serverUrl = TEST_SERVER;
    String userName = TEST_USERNAME;
    String userPassword = TEST_PASSWORD;
    char[] userSecret = TEST_SECRET;

    return WeaveAccountInfo.createWeaveAccountInfo(URI.create(serverUrl), userName, userPassword, userSecret);
  }

  public static WeaveAccountInfo getTestLoginInfo2() {
    return WeaveAccountInfo.createWeaveAccountInfo(
        URI.create(TEST_SERVER),
        "weavetest2",
        "foobarbang2",
        "foobarbang212".toCharArray()
    );
  }

  public static WeaveAccountInfo getInvalidLoginInfo() {
    String serverUrl = TEST_SERVER;
    String userName = "weavetestdontexist";
    String userPassword = TEST_PASSWORD;
    char[] userSecret = TEST_SECRET;
    return WeaveAccountInfo.createWeaveAccountInfo(URI.create(serverUrl), userName, userPassword, userSecret);
  }

  public static WeaveAccountInfo getTestBadPathLoginInfo() {
    String serverUrl = "https://www.woodworth.org/bad/path/to/nothing/";
    String userName = TEST_USERNAME;
    String userPassword = TEST_PASSWORD;
    char[] userSecret = TEST_SECRET;

    return WeaveAccountInfo.createWeaveAccountInfo(URI.create(serverUrl), userName, userPassword, userSecret);
  }

  public static WeaveAccountInfo getTestBadServerLoginInfo() {
    String serverUrl = "https://foobar.woodworth.org/bad/path/to/nothing/";
    String userName = TEST_USERNAME;
    String userPassword = TEST_PASSWORD;
    char[] userSecret = TEST_SECRET;

    return WeaveAccountInfo.createWeaveAccountInfo(URI.create(serverUrl), userName, userPassword, userSecret);
  }

  public static UserWeave getUserWeave(WeaveAccountInfo info) {
    return getWeaveFactory().createUserWeave(info);
  }

  private static final TestWeaveFactory sm_weaveFactory = new TestWeaveFactory();

  public static TestWeaveFactory getWeaveFactory() {
    return sm_weaveFactory;
  }

  public static class TestWeaveFactory {

    private final WeaveTransport m_delegate;

    private TestWeaveFactory() {
      m_delegate = new WeaveTransport();
    }

    public UserWeave createUserWeave(WeaveAccountInfo info) {
      return new UserWeave(getWeaveTransport(),
          info.getServer(),
          info.getUsername(),
          info.getPassword()
      );
    }

    private WeaveTransport getWeaveTransport() {
      return m_delegate;
    }
  }
}
