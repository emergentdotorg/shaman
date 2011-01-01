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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public class WeaveSessionTest {

//  @DataProvider(name = "test1")
  public Iterator<Object[]> createData() {
    ArrayList<Object[]> retval = new ArrayList<Object[]>();
    retval.add(getLoginInfoAsArray(TestUtil.getTestLoginInfo()));
    retval.add(getLoginInfoAsArray(TestUtil.getTestLoginInfo2()));
//    retval.add(getLoginInfoAsArray(WeaveTestUtil.getInvalidLoginInfo()));
    return retval.iterator();
  }

  private static Object[] getLoginInfoAsArray(WeaveAccountInfo info) {
    return new Object[] { info };
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

  private static QueryResult<List<WeaveBasicObject>> getCollection(UserWeave weave, String nodePath, QueryParams params) throws WeaveException {
    if (params == null)
      params = new QueryParams();
    URI uri = weave.buildSyncUriFromSubpath(nodePath + params.toQueryString());
    return weave.getWboCollection(uri);
  }

}
