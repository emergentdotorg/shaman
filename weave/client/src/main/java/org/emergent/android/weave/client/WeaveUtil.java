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

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * @author Patrick Woodworth
 */
public class WeaveUtil {

  private static final String JSON_STREAM_TYPE = "application/json";
  private static final String ENTITY_CHARSET_NAME = "UTF-8";

  private static final SecureRandom sm_random = new SecureRandom();

  private WeaveUtil() {
    // no instantiation
  }

  public static String legalizeUsername(String friendlyUsername) {
    return WeaveCryptoUtil.getInstance().legalizeUsername(friendlyUsername);
  }

  public static String createEntryId() {
//    try {
//      MessageDigest digest = MessageDigest.getInstance("SHA1");
      byte[] bytes = new byte[12];
      synchronized (sm_random) {
        sm_random.nextBytes(bytes);
      }
//      digest.update(bytes);
      byte[] baseEncodedBytes = Base32.encode(bytes);
      String retval = WeaveUtil.toAsciiString(baseEncodedBytes);
//      if (retval.length() > 12)
//        retval = retval.substring(0, 12);
      return retval;
//    } catch (GeneralSecurityException e) {
//      throw new Error(e);
//    }
  }


  @SuppressWarnings({"ThrowableInstanceNeverThrown"})
  public static void checkNull(URI uri) {
    if (uri == null) {
      Dbg.w(new IllegalArgumentException("checkNull(URI) had null arg"));
    } else if (uri.getHost() == null || uri.getHost().length() < 1) {
      Dbg.w(new IllegalArgumentException("checkNull(URI) had empty host"));
    }
  }

  @SuppressWarnings({"ThrowableInstanceNeverThrown"})
  public static void checkNull(String str) {
    if (str == null || str.trim().length() < 1) {
      Dbg.w(new IllegalArgumentException("checkNull(String) had empty arg"));
    }
  }

  public static String toModifiedTimeString(Date modified) {
    long time = modified.getTime();
    double timed = time / 1000.0;
    return String.format(Locale.ENGLISH, "%.2f", timed);
  }

  public static Date toModifiedTimeDate(String modified) {
    long now = System.currentTimeMillis();
    try {
      double modDouble = Double.parseDouble(modified) * 1000;
      long mod = Math.round(modDouble);
//      Dbg.printf("mod: %d ; cur : %d ; delta : %d\n", mod, now, now - mod);
      return new Date(mod);
    } catch (Exception e) {
      return new Date(); // todo buggy ?
    }
  }

  public static Date toModifiedTimeDate(double modDouble) {
    try {
      long mod = Math.round(modDouble * 1000);
      return new Date(mod);
    } catch (Exception e) {
      return null;
    }
  }

  public static UriBuilder buildUpon(URI serverUri) {
    return new UriBuilder(serverUri);
  }

  public static void dump(JSONObject jsonObject) {
    try {
      String out = jsonObject.toString(2);
      System.out.println(out);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public static String encodeUriSegment(String segment) {
    try {
      return URLEncoder.encode(segment, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String toString(URI uri) {
    checkNull(uri);
    String retval = uri == null ? null : uri.toString();
    checkNull(retval);
    return retval;
  }

  public static byte[] toAsciiBytes(String data) {
    try {
      return data == null ? null : data.getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String toAsciiString(byte[] data) {
    try {
      return data == null ? null : new String(data, "US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static byte[] toUtf8Bytes(String data) {
    try {
      return data == null ? null : data.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String toUtf8String(byte[] data) {
    try {
      return data == null ? null : new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void zeroize(char[] secret) {
    if (secret != null)
      Arrays.fill(secret, '\0');
  }

  private static HttpEntity toHttpEntity(JSONArray jsonArray) throws JSONException {
    try {
      StringEntity entity = new StringEntity(jsonArray.toString(0), ENTITY_CHARSET_NAME);
      entity.setContentType(JSON_STREAM_TYPE + HTTP.CHARSET_PARAM + ENTITY_CHARSET_NAME);
      return entity;
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static HttpEntity toHttpEntity(WeaveBasicObject wbo) throws JSONException {
    try {
      StringEntity entity = new StringEntity(wbo.toJSONObjectString(), ENTITY_CHARSET_NAME);
      entity.setContentType(JSON_STREAM_TYPE + HTTP.CHARSET_PARAM + ENTITY_CHARSET_NAME);
      return entity;
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static BulkKeyCouplet buildBulkKeyPair(String legalUsername, byte[] syncKey, JSONObject cryptoKeysPayload)
      throws GeneralSecurityException, WeaveException, JSONException {

      byte[] keyBytes = WeaveCryptoUtil.deriveSyncKey(syncKey, legalUsername);
      Key bulkKey = new SecretKeySpec(keyBytes, "AES");

      byte[] hmkeyBytes = WeaveCryptoUtil.deriveSyncHmacKey(syncKey, keyBytes, legalUsername);
      Key hmbulkKey = new SecretKeySpec(hmkeyBytes, "AES");

      JSONObject ckencPayload = decryptWboPayload(bulkKey, hmbulkKey, cryptoKeysPayload);

      JSONArray jsonArray = ckencPayload.getJSONArray("default");
      String bkey2str = jsonArray.getString(0);
      String bhmac2str = jsonArray.getString(1);
      byte[] bkey2bytes = Base64.decode(bkey2str);

      Key bulkKey2 = new SecretKeySpec(bkey2bytes, "AES");

      byte[] bhmac2bytes = Base64.decode(bhmac2str);

      Key bulkHmacKey2 = new SecretKeySpec(bhmac2bytes, "AES");

      return new BulkKeyCouplet(bulkKey2, bulkHmacKey2);
  }

  public static JSONObject decryptWboPayload(Key bulkKey, Key hmbulkKey, JSONObject weoObj)
      throws GeneralSecurityException, JSONException {

    String weoCipherText =  weoObj.getString("ciphertext");
    String weoIV = weoObj.getString("IV");
    String weoHmac = weoObj.getString("hmac");

    String plaintext = WeaveCryptoUtil.getInstance().decrypt(bulkKey, hmbulkKey, weoCipherText, weoIV, weoHmac);

    return new JSONObject(plaintext);
  }

  public static JSONObject encryptWboPayload(Key bulkKey, Key hmbulkKey, JSONObject payload)
      throws GeneralSecurityException, JSONException {

    byte[] ivBytes = new byte[16];
    sm_random.nextBytes(ivBytes);
    String iv = toAsciiString(Base64.encode(ivBytes));

    String plaintext = payload.toString();

    WeaveCryptoUtil cryptoUtil = WeaveCryptoUtil.getInstance();
    String ciphertext = cryptoUtil.encrypt(bulkKey, hmbulkKey, plaintext, iv);
    String hmac = cryptoUtil.createMac(hmbulkKey, ciphertext);
    cryptoUtil.checkMac(hmbulkKey, ciphertext, hmac);

    JSONObject retval = new JSONObject();
    retval.put("ciphertext", ciphertext);
    retval.put("IV", iv);
    retval.put("hmac", hmac);
    return retval;
  }

  public static boolean isEmpty(String s) {
    return (s == null || s.trim().length() == 0);
  }

  public static void close(InputStream closeable) {
    if (closeable != null) try {
      closeable.close();
    } catch (Exception ignored) {
    }
  }

  public static void close(OutputStream closeable) {
    if (closeable != null) try {
      closeable.close();
    } catch (Exception ignored) {
    }
  }

  public static void close(Reader closeable) {
    if (closeable != null) try {
      closeable.close();
    } catch (Exception ignored) {
    }
  }

  public static void close(Writer closeable) {
    if (closeable != null) try {
      closeable.close();
    } catch (Exception ignored) {
    }
  }

  public static void close(Connection closeable) {
    if (closeable != null) try {
      closeable.close();
    } catch (Exception ignored) {
    }
  }

  public static void close(Statement closeable) {
    if (closeable != null) try {
      closeable.close();
    } catch (Exception ignored) {
    }
  }

  public static void close(ResultSet closeable) {
    if (closeable != null) try {
      closeable.close();
    } catch (Exception ignored) {
    }
  }

  public static String readToString(File file) throws IOException {
    BufferedReader is = null;
    StringWriter os = null;
    try {
      is = new BufferedReader(new FileReader(file));
      os = new StringWriter();
      char[] buf = new char[512];
      int read = 0;
      while ((read = is.read(buf)) > -1) {
        if (read == 0) {
          Thread.yield();
        } else {
          os.write(buf, 0, read);
        }
      }
      return os.toString();
    } finally {
      close(is);
      close(os);
    }
  }


  public static String readToString(InputStream is) throws IOException {
    ByteArrayOutputStream os = null;
    try {
      os = new ByteArrayOutputStream();
      byte[] buf = new byte[512];
      int read = 0;
      while ((read = is.read(buf)) > -1) {
        if (read == 0) {
          Thread.yield();
        } else {
          os.write(buf, 0, read);
        }
      }
      return WeaveUtil.toUtf8String(os.toByteArray());
    } finally {
      close(os);
    }
  }

  public static class UriBuilder {

    private String m_val;

    public UriBuilder(URI uri) {
      m_val = uri.toASCIIString();
    }

    public void appendEncodedPath(String s) {
      if (m_val.charAt(m_val.length() - 1) != '/')
        m_val += "/";
      m_val += s;
    }

    public URI build() {
      try {
        return URI.create(m_val);
      } catch (IllegalArgumentException e) {
        Dbg.w("BAD URI: %s", m_val);
        throw e;
      }
    }
  }
}
