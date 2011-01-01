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
import org.emergent.android.weave.client.*;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public class UserWeaveTest extends TestCase {

  public void testEmailToUsername() throws Exception {
    WeaveCryptoUtil cryptoUtil = WeaveCryptoUtil.getInstance();
    assertEquals("2htrqwwwgltmo7jyf5mgmnzl3rjwfezw", cryptoUtil.legalizeUsername("weavetest@emergent.org"));
    assertEquals("2htrqwwwgltmo7jyf5mgmnzl3rjwfezw", cryptoUtil.legalizeUsername("WeAvetEst@emerGent.oRg"));
    assertEquals("weavetest-eme_rg97ent.org", cryptoUtil.legalizeUsername("weavetest-eme_rg97ent.org"));
  }

  public void testAuthenticateSuccess() throws Exception {
    WeaveAccountInfo loginInfo = TestUtil.getTestLoginInfo();
    UserWeave session = TestUtil.getUserWeave(loginInfo);
    session.authenticate();

    byte[] syncKey = Base32.decodeModified(TestUtil.TEST_SYNC_KEY);
    assertEquals("sync key length was not 16 bytes", 16, syncKey.length);

    UserWeave weave = session;

    BulkKeyCouplet bulkKeyPair = weave.getBulkKeyPair(syncKey);

    URI uri = weave.buildSyncUriFromSubpath(UserWeave.CollectionNode.STORAGE_PASSWORDS.nodePath + "?full=1");
    QueryResult<List<WeaveBasicObject>> recCol = weave.getWboCollection(uri);

    for (WeaveBasicObject rec : recCol.getValue()) {
      JSONObject encPayload = rec.getEncryptedPayload(bulkKeyPair.cipherKey, bulkKeyPair.hmacKey);
//      WeaveUtil.dump(encPayload);
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
}
