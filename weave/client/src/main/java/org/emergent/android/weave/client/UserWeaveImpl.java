package org.emergent.android.weave.client;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class UserWeaveImpl implements UserWeave {

  private final WeaveTransport m_transport;
  private final URI m_authUri;
  private final String m_userId;
  private final String m_password;
  private final String m_legalUsername;
  private final AtomicReference<URI> m_clusterUri;

  UserWeaveImpl(WeaveTransport transport, URI authUri, String userId, String password) {
    this(transport, authUri, userId, password, null);
  }

  protected UserWeaveImpl(WeaveTransport transport, URI authUri, String userId, String password, URI clusterUri) {
    m_authUri = authUri;
    m_userId = userId;
    m_legalUsername = WeaveCryptoUtil.getInstance().legalizeUsername(userId);
    m_password = password;
    m_transport = transport;
    m_clusterUri = new AtomicReference<URI>(clusterUri);
  }

  public void shutdown() {
    m_transport.shutdown();
  }

  public String getLegalUsername() {
    return m_legalUsername;
  }

  public final URI getClusterUri() throws WeaveException {
    return getClusterUri(true);
  }

  public final URI setClusterUri(URI clusterUri) {
    return m_clusterUri.getAndSet(clusterUri);
  }

  public final URI getClusterUri(boolean useCache) throws WeaveException {
    URI cached = null;
    if (useCache && ((cached = m_clusterUri.get()) != null))
      return cached;

    URI retval = getClusterUriSafe();
    m_clusterUri.compareAndSet(cached, retval);
    return retval;
  }

  public boolean checkUsernameAvailable() throws WeaveException {
    try {
      String nodePath = "/";
      String nodeStrVal = getUserNode(nodePath).getBody();
      return Integer.parseInt(nodeStrVal) == 0;
    } catch (NumberFormatException e) {
      throw new WeaveException(e);
    }
  }

  @Override
  public void authenticate() throws WeaveException {
    JSONObject jsonObj;
    jsonObj = getNode(HashNode.INFO_COLLECTIONS).getValue();
    jsonObj = getNode(HashNode.META_GLOBAL).getValue();
    jsonObj.has("foo");
  }

  @Override
  public void authenticateSecret(char[] secret) throws WeaveException {
    authenticate();
  }

  @Override
  public QueryResult<JSONObject> getNode(HashNode node) throws WeaveException {
    try {
      URI nodeUri = node.userServer
          ? buildUserUriFromSubpath(node.nodePath)
          : buildSyncUriFromSubpath(node.nodePath);
      WeaveResponse result = getNode(nodeUri);
      dumpResponse(result);
      String body = result.getBody();
      JSONObject obj = null;
      if (!"[]".equals(body))
        obj = new JSONObject(body);
      return new QueryResult<JSONObject>(result, obj);
    } catch (JSONException e) {
      throw new WeaveException(e);
    }
  }

  @Override
  public WeaveResponse putNode(String node, String body) throws WeaveException {
//    try {
      URI nodeUri = buildUserUriFromSubpath(node);
      WeaveResponse result = putNode(nodeUri, body);
      dumpResponse(result);
//      return new QueryResult<JSONObject>(result, new JSONObject(result.getBody()));
      return result;
//    } catch (JSONException e) {
//      throw new WeaveException(e);
//    }
  }

  public QueryResult<JSONObject> postNode(CollectionNode node, String body) throws WeaveException {
    try {
      URI nodeUri = buildSyncUriFromSubpath(node.nodePath);
      WeaveResponse result = postNode(nodeUri, body);
      dumpResponse(result);
      return new QueryResult<JSONObject>(result, new JSONObject(result.getBody()));
    } catch (JSONException e) {
      throw new WeaveException(e);
    }
  }

  @Override
  public QueryResult<List<WeaveBasicObject>> getWboCollection(URI uri) throws WeaveException {
    try {
      WeaveResponse response = getNode(uri);
      dumpResponse(response);
      QueryResult<List<WeaveBasicObject>> result = new QueryResult<List<WeaveBasicObject>>(response);
      JSONArray jsonPassArray = new JSONArray(response.getBody());
      List<WeaveBasicObject> records = new ArrayList<WeaveBasicObject>();
      for (int ii = 0; ii < jsonPassArray.length(); ii++) {
        JSONObject jsonObj = jsonPassArray.getJSONObject(ii);
        WeaveBasicObject wbo = new WeaveBasicObject(uri, jsonObj);
        records.add(wbo);
      }
      result.setValue(records);
      return result;
    } catch (JSONException e) {
      throw new WeaveException(e);
    }
  }

  @Override
  public BulkKeyTool getBulkTool(char[] secret) throws GeneralSecurityException, WeaveException {
    try {
      byte[] syncKey = Base32.decodeModified(new String(secret)); // todo don't convert to string
      return new BulkKeyTool(getBulkKeyPair(syncKey));
    } catch (GeneralSecurityException e) {
      throw new WeaveException(e);
    }
  }

  private URI getClusterUriSafe() {
    URI retval = m_authUri;
    try {
      URI unsafeResult = getClusterUriUnsafe();
      if (unsafeResult != null)
        retval = unsafeResult;
    } catch (Exception ignored) {
//      Dbg.v(e);
    }
    return retval;
  }

  private URI getClusterUriUnsafe() throws WeaveException {
    try {
      String nodePath = "/node/weave";
      String nodeWeaveVal = getUserNode(nodePath).getBody();
      return new URI(nodeWeaveVal);
    } catch (URISyntaxException e) {
      throw new WeaveException(e);
    }
  }

  protected RSAPublicKey getPublicKey() throws WeaveException {
    try {
      URI nodeUri = buildSyncUriFromSubpath("/storage/keys/pubkey");
      WeaveBasicObject nodeObj = new WeaveBasicObject(nodeUri, new JSONObject(getNode(nodeUri).getBody()));
      JSONObject payloadObj = nodeObj.getPayload();
      String pubKey = payloadObj.getString("keyData");
      return WeaveCryptoUtil.getInstance().readCertificatePubKey(pubKey);
    } catch (GeneralSecurityException e) {
      throw new WeaveException(e);
    } catch (JSONException e) {
      throw new WeaveException(e);
    }
  }

  @Override
  public BulkKeyCouplet getBulkKeyPair(byte[] syncKey) throws GeneralSecurityException, WeaveException {
    try {

      JSONObject cryptoKeysPayload = getCryptoKeys();

      String legalUsername = getLegalUsername();

      return WeaveUtil.buildBulkKeyPair(legalUsername, syncKey, cryptoKeysPayload);


//      byte[] keyBytes = WeaveCryptoUtil.deriveSyncKey(syncKey, getLegalUsername());
//      Key bulkKey = new SecretKeySpec(keyBytes, "AES");
//
//      byte[] hmkeyBytes = WeaveCryptoUtil.deriveSyncHmacKey(syncKey, keyBytes, getLegalUsername());
//      Key hmbulkKey = new SecretKeySpec(hmkeyBytes, "AES");
//
//      JSONObject ckwbojsonobj = getCryptoKeys();
//
//      WeaveBasicObject.WeaveEncryptedObject weo = new WeaveBasicObject.WeaveEncryptedObject(ckwbojsonobj);
//      JSONObject ckencPayload = weo.decryptObject(bulkKey, hmbulkKey);
//
//      JSONArray jsonArray = ckencPayload.getJSONArray("default");
//      String bkey2str = jsonArray.getString(0);
//      String bhmac2str = jsonArray.getString(1);
//      byte[] bkey2bytes = Base64.decode(bkey2str);
//
//      Key bulkKey2 = new SecretKeySpec(bkey2bytes, "AES");
//
//      byte[] bhmac2bytes = Base64.decode(bhmac2str);
//
//      Key bulkHmacKey2 = new SecretKeySpec(bhmac2bytes, "AES");
//
//      return new BulkKeyCouplet(bulkKey2, bulkHmacKey2);
    } catch (JSONException e) {
      throw new WeaveException(e);
    }
  }

  @Override
  public JSONObject getCryptoKeys() throws WeaveException {
    try {
      URI nodeUri = buildSyncUriFromSubpath("/storage/crypto/keys");
      WeaveResponse response = getNode(nodeUri);
      dumpResponse(response);
      WeaveBasicObject nodeObj = new WeaveBasicObject(nodeUri, new JSONObject(response.getBody()));
      return nodeObj.getPayload();
    } catch (JSONException e) {
      throw new WeaveException(e);
    }
  }

  protected final WeaveResponse getUserNode(String path) throws WeaveException {
    URI nodeUri = buildUserUriFromSubpath(path);
    return getNode(nodeUri);
  }

  public final WeaveResponse getNode(URI nodeUri) throws WeaveException {
    try {
      return m_transport.execGetMethod(getLegalUsername(), m_password, nodeUri);
    } catch (IOException e) {
      throw new WeaveException(e);
    }
  }

  public final WeaveResponse putNode(URI nodeUri, String body) throws WeaveException {
    try {
      StringEntity entity = new StringEntity(body);
      return m_transport.execPutMethod(getLegalUsername(), m_password, nodeUri, entity);
    } catch (IOException e) {
      throw new WeaveException(e);
    }
  }

  public final WeaveResponse postNode(URI nodeUri, String body) throws WeaveException {
    try {
      StringEntity entity = new StringEntity(body);
      return m_transport.execPostMethod(getLegalUsername(), m_password, nodeUri, entity);
    } catch (IOException e) {
      throw new WeaveException(e);
    }
  }

  public final WeaveResponse deleteNode(URI nodeUri) throws WeaveException {
    try {
      return m_transport.execDeleteMethod(getLegalUsername(), m_password, nodeUri);
    } catch (IOException e) {
      throw new WeaveException(e);
    }
  }

  public URI buildUserUriFromSubpath(String subpath) {
    return buildUserUriFromSubpath(m_authUri, getLegalUsername(), subpath);
  }

  public URI buildSyncUriFromSubpath(String subpath) throws WeaveException {
    return buildSyncUriFromSubpath(getClusterUri(), getLegalUsername(), subpath);
  }

  protected static URI buildUserUriFromSubpath(URI authUri, String userId, String subpath) {
    WeaveUtil.checkNull(authUri);
    WeaveUtil.UriBuilder builder = WeaveUtil.buildUpon(authUri);
    builder.appendEncodedPath("user/" + WeaveConstants.WEAVE_API_VERSION + "/" + userId);
    while (subpath.startsWith("/")) {
      subpath = subpath.substring(1);
    }
    builder.appendEncodedPath(subpath);
    return builder.build();
  }

  protected static URI buildSyncUriFromSubpath(URI clusterUri, String userId, String subpath) {
    return buildSyncUriFromSubpath(clusterUri, userId, null, subpath);
  }

  protected static URI buildSyncUriFromSubpath(URI clusterUri, String userId, QueryParams params, String pathSection) {
    String subpath = pathSection;
    if (params != null)
      subpath = subpath + params.toQueryString();
    WeaveUtil.checkNull(clusterUri);
    WeaveUtil.UriBuilder builder = WeaveUtil.buildUpon(clusterUri);
    builder.appendEncodedPath(WeaveConstants.WEAVE_API_VERSION + "/" + userId);
    while (subpath.startsWith("/")) {
      subpath = subpath.substring(1);
    }
    builder.appendEncodedPath(subpath);
    return builder.build();
  }

  private static void dumpResponse(WeaveResponse response) {
    if (true)
      return;
    WeaveTransport.WeaveResponseHeaders headers = response.getResponseHeaders();
    for (Header header : headers.getHeaders()) {
      System.out.println("" + header.toString());
    }
    String body = response.getBody();
    System.out.println(body);
  }
}
