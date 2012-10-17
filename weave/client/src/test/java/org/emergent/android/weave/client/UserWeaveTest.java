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

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public class UserWeaveTest extends TestCase {

//  public static final String TEST_SERVER = System.getenv("WEAVE_TEST_SERVERURL");
//  public static final String TEST_USERNAME = System.getenv("WEAVE_TEST_USERNAME");
//  public static final String TEST_PASSWORD = System.getenv("WEAVE_TEST_PASSWORD");
//  public static final String TEST_SYNC_KEY = System.getenv("WEAVE_TEST_SYNCKEY");

//  public static final String TEST_SERVER = "http://localhost:8080/weave/";

  public static final String TEST_SERVER = "https://auth.services.mozilla.com/";
  public static final String TEST_USERNAME = "weavetest@emergent.org";
  public static final String TEST_PASSWORD = "foobarbang";
  public static final String TEST_SYNC_KEY = "xrwjwg42i6q9ds6ctwhudb9x24";
  private static final String TEST_CK_PAYLOAD = "{\"hmac\":\"a1002a0f60dec88b64cca454aaa53d8b52c70eefdb95df06606898698d8132e0\",\"ciphertext\":\"ZAevUYrqAGuSjgeifEmLCrbJuNu/HIPaiPyJx1NTpK7wPBzkB+C0gr2xsHLtIK3hQX4GhWRueded6xtwUYy+TmICZ1BiloslPDMIKS5Xa6wE4x0nJmMJT/rnrb1RcKCpTzRy6z3rqYj+953Ui1cgFLw+PVFcFux99v/x/ZviQ2JvzzwNRSHy5KCuzKf6KPKo3usEvbiCFEKiMFcjJGCoPA==\",\"IV\":\"Zw4ISp4Qaxkmo59g0LOiHw==\"}";

//  public static final String TEST_SERVER = "https://auth.services.mozilla.com/";
//  public static final String TEST_USERNAME = "newweave@emergent.org";
//  public static final String TEST_PASSWORD = "foobarbang";
//  public static final String TEST_SYNC_KEY = "7-2u5ps-9h34s-drv9n-qx582-zs5vi";

  public static final char[] TEST_SECRET = TEST_SYNC_KEY.toCharArray();

  static {
    TestLoggingConfigurator.initLogging();
  }

  private static final TestWeaveFactory sm_weaveFactory = new TestWeaveFactory();


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

  public static UserWeave getUserWeave(WeaveAccountInfo info) {
    return getWeaveFactory().createUserWeave(info);
  }

  public static TestWeaveFactory getWeaveFactory() {
    return sm_weaveFactory;
  }

  public void testEmailToUsername() throws Exception {
    WeaveCryptoUtil cryptoUtil = WeaveCryptoUtil.getInstance();
    assertEquals("2htrqwwwgltmo7jyf5mgmnzl3rjwfezw", cryptoUtil.legalizeUsername("weavetest@emergent.org"));
    assertEquals("2htrqwwwgltmo7jyf5mgmnzl3rjwfezw", cryptoUtil.legalizeUsername("WeAvetEst@emerGent.oRg"));
    assertEquals("weavetest-eme_rg97ent.org", cryptoUtil.legalizeUsername("weavetest-eme_rg97ent.org"));
  }

  public void testAuthenticateSuccess() throws Exception {
    WeaveAccountInfo loginInfo = getTestLoginInfo();
    UserWeave session = getUserWeave(loginInfo);
    session.authenticate();

    byte[] syncKey = Base32.decodeModified(TEST_SYNC_KEY);
    assertEquals("sync key length was not 16 bytes", 16, syncKey.length);

    UserWeave weave = session;

    BulkKeyCouplet bulkKeyPair = weave.getBulkKeyPair(syncKey);

    URI uri = weave.buildSyncUriFromSubpath(UserWeave.CollectionNode.STORAGE_BOOKMARKS.nodePath + "?full=1");
    QueryResult<List<WeaveBasicObject>> recCol = weave.getWboCollection(uri);

    {
//      URI uri2 = weave.buildSyncUriFromSubpath(UserWeave.CollectionNode.STORAGE_CLIENTS.nodePath + "?full=1");
//      QueryResult<List<WeaveBasicObject>> recCol2 = weave.getWboCollection(uri2);

//      QueryResult<JSONObject> recCol3 = weave.putNode(UserWeave.CollectionNode.STORAGE_CLIENTS, clientsVal);
    }

    for (WeaveBasicObject rec : recCol.getValue()) {
      JSONObject encPayload = rec.getEncryptedPayload(bulkKeyPair.cipherKey, bulkKeyPair.hmacKey);
      JSONObject totalObj = rec.toJSONObject();
      totalObj.put("payload", encPayload);
      dump(totalObj, 0);
    }

  }

  public void testOfflineDecrypt() throws Exception {
    WeaveAccountInfo loginInfo = getTestLoginInfo();
    UserWeave weave = getUserWeave(loginInfo);
    weave.authenticate();

    byte[] syncKey = Base32.decodeModified(TEST_SYNC_KEY);

//    JSONObject cryptoKeysPayload = new JSONObject(TEST_CK_PAYLOAD);
    JSONObject cryptoKeysPayload = weave.getCryptoKeys();
    String legalUsername = WeaveUtil.legalizeUsername(loginInfo.getUsername());
    BulkKeyCouplet bulkKeyPair = WeaveUtil.buildBulkKeyPair(legalUsername, syncKey, cryptoKeysPayload);

    URI uri = weave.buildSyncUriFromSubpath(UserWeave.CollectionNode.STORAGE_BOOKMARKS.nodePath + "?full=1");

    WeaveResponse response = weave.getNode(uri);
    JSONArray jsonPassArray = new JSONArray(response.getBody());
    for (int ii = 0; ii < jsonPassArray.length(); ii++) {
      JSONObject jsonObj = jsonPassArray.getJSONObject(ii);
      JSONObject wboEncPayload = new JSONObject(jsonObj.getString("payload"));
      JSONObject wboDecPayload = WeaveUtil.decryptWboPayload(bulkKeyPair.cipherKey, bulkKeyPair.hmacKey, wboEncPayload);
      JSONObject totalObj = jsonObj;
      totalObj.put("payload", wboDecPayload);
      dump(totalObj, 0);
    }
  }

  public static void dump(JSONObject jsonObject, int indent) {
    try {
      String out = jsonObject.toString(indent);
      System.out.println("modified: " + jsonObject.getDouble("modified") + " " + out.replaceAll("\\n", ""));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }



/*
  public void testAuthenticateFailure() throws Exception {
    try {
      WeaveAccountInfo loginInfo = TestUtil.getInvalidLoginInfo();
      UserWeave session = TestUtil.getUserWeave(loginInfo);
      session.authenticate();
      fail("Should have thrown an exception");
    } catch (WeaveException e) {
      HttpResponseException cause = (HttpResponseException)e.getCause();
      assertEquals(cause.getStatusCode(), WeaveConstants.UNAUTHORIZED_HTTP_STATUS_CODE);
    }
  }

  public void testAuthenticateBadPath() throws Exception {
    WeaveAccountInfo loginInfo = TestUtil.getTestBadPathLoginInfo();
    UserWeave session = TestUtil.getUserWeave(loginInfo);
    session.authenticate();
  }

  public void testAuthenticateBadServer() throws Exception {
    WeaveAccountInfo loginInfo = TestUtil.getTestBadServerLoginInfo();
    UserWeave session = TestUtil.getUserWeave(loginInfo);
    session.authenticate();
  }
*/


  //  @DataProvider(name = "test1")
  public Iterator<Object[]> createData() {
    ArrayList<Object[]> retval = new ArrayList<Object[]>();
    retval.add(getLoginInfoAsArray(getTestLoginInfo()));
    retval.add(getLoginInfoAsArray(getTestLoginInfo2()));
//    retval.add(getLoginInfoAsArray(WeaveTestUtil.getInvalidLoginInfo()));
    return retval.iterator();
  }

  private static Object[] getLoginInfoAsArray(WeaveAccountInfo info) {
    return new Object[]{info};
  }


  private static QueryResult<List<WeaveBasicObject>> getCollection(UserWeave weave, String nodePath, QueryParams params) throws WeaveException {
    if (params == null)
      params = new QueryParams();
    URI uri = weave.buildSyncUriFromSubpath(nodePath + params.toQueryString());
    return weave.getWboCollection(uri);
  }

//  @Test
//  public void testAuthentication() throws Exception {
//    WeaveAccountInfo loginInfo = TestUtil.getTestLoginInfo();
//    AbstractKeyManager session = TestUtil.getWeaveSession(loginInfo);
//    assertTrue( !session.checkUsernameAvailable() );
//  }
//
//  @Test
//  public void testInvalidAuthentication() throws Exception {
//    WeaveAccountInfo loginInfo = TestUtil.getInvalidLoginInfo();
//    AbstractKeyManager session = TestUtil.getWeaveSession(loginInfo);
//    assertTrue( session.checkUsernameAvailable() );
//  }
//
//  @Test
//  public void testQueryParams() throws Exception {
//    WeaveAccountInfo loginInfo = TestUtil.getTestLoginInfo();
//    AbstractKeyManager session = TestUtil.getWeaveSession(loginInfo);
//    Date latestDate = null;
//    Date penultimateDate = null;
//    int totalRecords = 0;
//    {
//      QueryParams qParams = new QueryParams();
//      QueryResult<List<WeaveBasicObject>> qResult = getCollection(session, "/storage/bookmarks",qParams);
//      List<WeaveBasicObject> records = qResult.getValue();
//      for (WeaveBasicObject record : records) {
//        totalRecords++;
//        Date modifiedDate = record.getModifiedDate();
//        WeaveUtil.toModifiedTimeString(modifiedDate);
//        if (latestDate == null) {
//          latestDate = modifiedDate;
//          penultimateDate = modifiedDate;
//        } else if (latestDate.before(modifiedDate)) {
//          penultimateDate = latestDate;
//          latestDate = modifiedDate;
//        } else if (latestDate.before(penultimateDate)) {
//          penultimateDate = modifiedDate;
//        }
//      }
//    }
//    Dbg.w("TOTAL BOOKMARKS: " + totalRecords);
//    {
//      QueryParams qParams = (new QueryParams()).setOlder(latestDate);
//      QueryResult<List<WeaveBasicObject>> qResult = getCollection(session, "/storage/bookmarks",qParams);
//      List<WeaveBasicObject> records = qResult.getValue();
////      assertEquals(totalRecords - 1, records.size());
//    }
////    {
////      WeaveQueryParams qParams = (new WeaveQueryParams()).setNewer(penultimateDate);
////      WeaveQueryResult<WeaveBookmarkRecord> qResult = session.getBookmarks(qParams);
////      List<WeaveBookmarkRecord> records = qResult.getRecords();
////      assertEquals(1, records.size());
////    }
//  }
//
//  @Test(dataProvider = "test1")
//  public void testQueryPasswords(WeaveAccountInfo loginInfo) throws Exception {
//    AbstractKeyManager session = TestUtil.getWeaveSession(loginInfo);
//    int totalRecords = 0;
//    QueryParams qParams = new QueryParams();
//    QueryResult<List<WeaveBasicObject>> qResult = getCollection(session, "/storage/passwords",qParams);
//    List<WeaveBasicObject> records = qResult.getValue();
//    for (WeaveBasicObject record : records) {
//      totalRecords++;
//      Date modifiedDate = record.getModifiedDate();
//      WeaveUtil.toModifiedTimeString(modifiedDate);
//    }
//  }

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
