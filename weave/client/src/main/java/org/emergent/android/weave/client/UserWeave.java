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

import org.json.JSONObject;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public interface UserWeave {

  URI buildUserUriFromSubpath(String subpath);

  URI buildSyncUriFromSubpath(String subpath) throws WeaveException;

  WeaveResponse getNode(URI uri) throws WeaveException;

  WeaveResponse putNode(URI nodeUri, String body) throws WeaveException;

  WeaveResponse postNode(URI nodeUri, String body) throws WeaveException;

  WeaveResponse deleteNode(URI nodeUri) throws WeaveException;

  QueryResult<List<WeaveBasicObject>> getWboCollection(URI uri) throws WeaveException;

  void authenticate() throws WeaveException;

  JSONObject getCryptoKeys() throws WeaveException;

  BulkKeyCouplet getBulkKeyPair(byte[] syncKey) throws GeneralSecurityException, WeaveException;

  void authenticateSecret(char[] secret) throws WeaveException;

  QueryResult<JSONObject> getNode(HashNode node) throws WeaveException;

  BulkKeyTool getBulkTool(char[] secret) throws GeneralSecurityException, WeaveException;

  WeaveResponse putNode(String node, String body) throws WeaveException;

}
