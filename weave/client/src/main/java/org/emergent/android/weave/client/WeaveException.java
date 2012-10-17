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

import org.apache.http.client.HttpResponseException;

import java.security.GeneralSecurityException;

/**
 * @author Patrick Woodworth
 */
public class WeaveException extends Exception {

  public static final int GENERAL_CODE = 1;
  public static final int NOTFOUND_CODE = 2;
  public static final int UNAUTHORIZED_CODE = 3;
  public static final int CRYPTO_CODE = 4;
  private static final int BACKOFF_CODE = 5;

  private final int m_errorCode;

  public WeaveException() {
    this(GENERAL_CODE);
  }

  private WeaveException(int type) {
    m_errorCode = type;
  }

  public WeaveException(String message) {
    this(GENERAL_CODE, message);
  }

  private WeaveException(int type, String message) {
    super(message);
    m_errorCode = type;
  }

  public WeaveException(Throwable cause) {
    this(getExceptionType(cause), cause);
  }

  private WeaveException(int type, Throwable cause) {
    super(cause);
    m_errorCode = type;
  }

  public WeaveException(String message, Throwable cause) {
    this(getExceptionType(cause), message, cause);
  }

  private WeaveException(int type, String message, Throwable cause) {
    super(message, cause);
    m_errorCode = type;
  }

  public int getErrorCode() {
    return m_errorCode;
  }

  private static boolean isAuthFailure(HttpResponseException e) {
    int statusCode = e.getStatusCode();
    if (WeaveConstants.UNAUTHORIZED_HTTP_STATUS_CODE == statusCode)
      return true;
    return false;
  }

  private static int getExceptionType(Throwable cause) {
    if (cause instanceof WeaveTransport.WeaveResponseException) {
      WeaveTransport.WeaveResponseException e = (WeaveTransport.WeaveResponseException)cause;
      if (isAuthFailure(e)) {
        return UNAUTHORIZED_CODE;
      }
      if (e.getStatusCode() == WeaveConstants.NOT_FOUND_HTTP_STATUS_CODE) {
        return NOTFOUND_CODE;
      }
    }
    if (cause instanceof GeneralSecurityException) {
      return CRYPTO_CODE;
    }
    return GENERAL_CODE;
  }
}
