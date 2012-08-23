/*
 * Copyright 2011 Patrick Woodworth
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Properties;
import static org.emergent.android.weave.client.WeaveMain.CipherMode.*;

/**
 * @author Patrick Woodworth
 */
public class WeaveMain {

  public enum CipherMode {
    RAW,
    NONE,
    DECRYPT,
    ENCRYPT
  }

  public static final String TEST_SERVER = "http://localhost:8080/weave/";
  public static final String TEST_USERNAME = "weavetest@emergent.org";
  public static final String TEST_PASSWORD = "foobarbang";
  public static final String TEST_SYNC_KEY = "xrwjwg42i6q9ds6ctwhudb9x24";

  private static final int INDENT = Integer.getInteger("indent", -1);

  private static final Properties smLoginProperties = new Properties();

  private static final WeaveFactory smWeaveFactory = new WeaveFactory(true);

  private static CipherMode smCipherMode =
      WeaveMain.CipherMode.valueOf(System.getProperty("mode", DECRYPT.name()).toUpperCase());

  private static JSONObject smCryptoKeysObj;

  private static BulkKeyCouplet smBulkKeyPair;

  private static String getUserEmail() {
    return smLoginProperties.getProperty("login.authAccount", TEST_USERNAME);
  }

  private static String getServerUrl() {
    return smLoginProperties.getProperty("login.server_url", TEST_SERVER);
  }

  private static String getPassword() {
    return smLoginProperties.getProperty("login.password", TEST_PASSWORD);
  }

  private static String getSyncKey() {
    return smLoginProperties.getProperty("login.sync_key", TEST_SYNC_KEY);
  }

  private static void loadLoginProperties() {
    InputStream in = null;
    try {
      String credsPath = System.getProperty("weavecreds", System.getenv("WEAVE_CREDS"));
      if (credsPath == null)
        return;

      File credsFile = new File(credsPath);
      if (!credsFile.isFile()  || credsFile.length() < 1)
        return;

      in = new FileInputStream(credsFile);
      smLoginProperties.load(in);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      WeaveUtil.close(in);
    }
  }

  private static synchronized JSONObject getCryptoKeys(SlimWeave weave) throws WeaveException {
    if (smCryptoKeysObj == null) {
      try {
        String respBody = null;
        String keysFilePath = System.getProperty("weavekeys", System.getenv("WEAVE_KEYS"));
        if (keysFilePath != null) {
          try {
            File keysFile = new File(keysFilePath);
            respBody = WeaveUtil.readToString(keysFile);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (respBody == null) {
          URI nodeUri = weave.buildSyncUriFromSubpath("/storage/crypto/keys");
          WeaveResponse response = weave.getNode(nodeUri);
          respBody = response.getBody();
        }
        smCryptoKeysObj = new JSONObject(respBody);
      } catch (JSONException e) {
        throw new WeaveException(e);
      }
    }
    return smCryptoKeysObj;
  }

  private static synchronized BulkKeyCouplet getBulKKeyPair(SlimWeave weave) throws WeaveException, GeneralSecurityException, JSONException {
    if (smBulkKeyPair == null) {
      String userEmail = getUserEmail();
      String userLogin = WeaveUtil.legalizeUsername(userEmail);
      byte[] syncKey = Base32.decodeModified(getSyncKey());
      JSONObject cryptoKeysObj = getCryptoKeys(weave);
      JSONObject cryptoKeysPayload = new JSONObject(cryptoKeysObj.getString("payload"));
      smBulkKeyPair = WeaveUtil.buildBulkKeyPair(userLogin, syncKey, cryptoKeysPayload);
    }
    return smBulkKeyPair;
  }

  public static void retrieveAndDump(String nodepath) throws Exception {
    String userEmail = getUserEmail();
    SlimWeave weave = smWeaveFactory.createUserWeave(URI.create(getServerUrl()), userEmail, getPassword());
    BulkKeyCouplet bulkKeyPair = getBulKKeyPair(weave);

    String respBody;
    if (nodepath != null) {
      URI uri = weave.buildSyncUriFromSubpath(nodepath);
      System.err.println("GETURI: " + uri.toASCIIString() );

      if (nodepath.startsWith("/storage/crypto/keys")) {
        dump(getCryptoKeys(weave));
        return;
      }

      WeaveResponse response = weave.getNode(uri);
      respBody = response.getBody();
    } else {
      respBody = WeaveUtil.readToString(System.in);
    }

    if (respBody.startsWith("[")) {
      System.err.println("JSONARR:");
       JSONArray jsonNewArray = new JSONArray();
       JSONArray jsonOldArray = new JSONArray(respBody);
       for (int ii = 0; ii < jsonOldArray.length(); ii++) {
         JSONObject jsonObj = jsonOldArray.optJSONObject(ii);
         if (jsonObj != null) {
           jsonObj = safeSwapForCiphered(jsonObj, bulkKeyPair);
           jsonNewArray.put(jsonObj);
         } else {
           System.err.println("WARNING array element " + ii + " was not a JSONObject");
         }
       }
       dump(jsonNewArray);
    } else if (respBody.startsWith("{")) {
      System.err.println("JSONOBJ:");
      JSONObject jsonObj = new JSONObject(respBody);
      jsonObj = safeSwapForCiphered(jsonObj, bulkKeyPair);
      dump(jsonObj);
    } else {
      System.err.println("NONJSON:");
      System.out.println(respBody);
    }
  }

  public static void doPut(String nodepath) throws Exception {
    String userEmail = getUserEmail();
    SlimWeave weave = smWeaveFactory.createUserWeave(URI.create(getServerUrl()), userEmail, getPassword());
    BulkKeyCouplet bulkKeyPair = getBulKKeyPair(weave);

    String sendBody = WeaveUtil.readToString(System.in);

    String respBody;
    if (nodepath != null) {
      URI uri = weave.buildSyncUriFromSubpath(nodepath);
      System.err.println("PUTURI: " + uri.toASCIIString() );

      if (nodepath.startsWith("/storage/crypto/keys")) {
        throw new Exception("You don't want this");
      }

      WeaveResponse response = weave.putNode(uri, sendBody);
      respBody = response.getBody();
    } else {
      respBody = sendBody;
    }

    System.err.println("RESPONSE:");
    System.out.println(respBody);
  }

  public static String retrieveRaw(String nodepath) throws Exception {
    String userEmail = getUserEmail();
    SlimWeave weave = smWeaveFactory.createUserWeave(URI.create(getServerUrl()), userEmail, getPassword());
    URI uri = weave.buildSyncUriFromSubpath(nodepath);
    System.err.println("GETURI: " + uri.toASCIIString() );
    WeaveResponse response = weave.getNode(uri);
    return response.getBody();
  }

  public static JSONObject safeSwapForCiphered(JSONObject jsonObj, BulkKeyCouplet bulkKeyPair) {
    return safeSwapForCiphered(jsonObj, bulkKeyPair, smCipherMode);
  }

  public static JSONObject safeSwapForCiphered(JSONObject jsonObj, BulkKeyCouplet bulkKeyPair, CipherMode cipherMode) {
    JSONObject retval = null;
    try {
      retval = swapForCiphered(jsonObj, bulkKeyPair, cipherMode);
    } catch (Exception e) {
      System.err.println("FAILURE " + cipherMode + " SWAPPING");
      e.printStackTrace();
    }
    if (retval == null)
      retval = jsonObj;
    return retval;
  }

  private static JSONObject swapForCiphered(JSONObject jsonObj, BulkKeyCouplet bulkKeyPair, CipherMode cipherMode)
      throws JSONException, GeneralSecurityException {

    if (cipherMode == NONE || cipherMode == RAW)
        return jsonObj;

    String oldPayloadStr = jsonObj.optString("payload", null);
    if (oldPayloadStr == null)
        return jsonObj;

    // todo what about jsonarrays

    JSONObject wboOldPayload = new JSONObject(oldPayloadStr);
    JSONObject wboNewPayload = (cipherMode == ENCRYPT)
        ? WeaveUtil.encryptWboPayload(bulkKeyPair.cipherKey, bulkKeyPair.hmacKey, wboOldPayload)
        : WeaveUtil.decryptWboPayload(bulkKeyPair.cipherKey, bulkKeyPair.hmacKey, wboOldPayload);

    JSONObject newNodeObj = new JSONObject(jsonObj.toString());
    if (cipherMode == ENCRYPT) {
      newNodeObj.put("payload",  wboNewPayload.toString());
    } else {
      newNodeObj.put("payload",  wboNewPayload);
    }
    return newNodeObj;
  }

  public static void dump(JSONObject jsonObject) {
    try {
      String out = INDENT > -1 ? jsonObject.toString(INDENT) : jsonObject.toString();
      System.out.println(out);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void dump(JSONArray jsonObject) {
    try {
      String out = INDENT > -1 ? jsonObject.toString(INDENT) : jsonObject.toString();
      System.out.println(out);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static void main(String[] args) throws Exception {
    String nodepath = null;
    if (args.length > 0)
      nodepath = args[0];

    loadLoginProperties();

    String method = System.getProperty("method", "get");
    if ("put".equals(method)) {
      doPut(nodepath);
    } else {
      if (smCipherMode == RAW) {
        System.out.println(retrieveRaw(nodepath));
      } else {
        retrieveAndDump(nodepath);
      }
    }
  }
}
