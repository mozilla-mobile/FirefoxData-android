/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa;

import org.json.simple.JSONArray;
import org.mozilla.gecko.background.fxa.FxAccountClientException.FxAccountClientMalformedResponseException;
import org.mozilla.gecko.background.fxa.FxAccountClientException.FxAccountClientRemoteException;
import org.mozilla.gecko.Locales;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.HKDF;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.SyncResponse;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import javax.crypto.Mac;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpHeaders;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.mozilla.sync.impl.FirefoxSyncInterModuleReceiver;

/**
 * An HTTP client for talking to an FxAccount server.
 * <p>
 * <p>
 * The delegate structure used is a little different from the rest of the code
 * base. We add a <code>RequestDelegate</code> layer that processes a typed
 * value extracted from the body of a successful response.
 */
public class FxAccountClient20 implements FxAccountClient {
  protected static final String LOG_TAG = FxAccountClient20.class.getSimpleName();

  protected static final String ACCEPT_HEADER = "application/json;charset=utf-8";

  public static final String JSON_KEY_EMAIL = "email";
  public static final String JSON_KEY_KEYFETCHTOKEN = "keyFetchToken";
  public static final String JSON_KEY_SESSIONTOKEN = "sessionToken";
  public static final String JSON_KEY_UID = "uid";
  public static final String JSON_KEY_VERIFIED = "verified";
  public static final String JSON_KEY_ERROR = "error";
  public static final String JSON_KEY_MESSAGE = "message";
  public static final String JSON_KEY_INFO = "info";
  public static final String JSON_KEY_CODE = "code";
  public static final String JSON_KEY_ERRNO = "errno";
  public static final String JSON_KEY_EXISTS = "exists";

  protected static final String[] requiredErrorStringFields = { JSON_KEY_ERROR, JSON_KEY_MESSAGE, JSON_KEY_INFO };
  protected static final String[] requiredErrorLongFields = { JSON_KEY_CODE, JSON_KEY_ERRNO };

  /**
   * The server's URI.
   * <p>
   * We assume throughout that this ends with a trailing slash (and guarantee as
   * much in the constructor).
   */
  protected final String serverURI;

  protected final Executor executor;

  public FxAccountClient20(String serverURI, Executor executor) {
    if (serverURI == null) {
      throw new IllegalArgumentException("Must provide a server URI.");
    }
    if (executor == null) {
      throw new IllegalArgumentException("Must provide a non-null executor.");
    }
    this.serverURI = serverURI.endsWith("/") ? serverURI : serverURI + "/";
    if (!this.serverURI.endsWith("/")) {
      throw new IllegalArgumentException("Constructed serverURI must end with a trailing slash: " + this.serverURI);
    }
    this.executor = executor;
  }

  protected BaseResource getBaseResource(String path, Map<String, String> queryParameters) throws UnsupportedEncodingException, URISyntaxException {
    if (queryParameters == null || queryParameters.isEmpty()) {
      return getBaseResource(path);
    }
    final String[] array = new String[2 * queryParameters.size()];
    int i = 0;
    for (Entry<String, String> entry : queryParameters.entrySet()) {
      array[i++] = entry.getKey();
      array[i++] = entry.getValue();
    }
    return getBaseResource(path, array);
  }

  /**
   * Create <code>BaseResource</code>, encoding query parameters carefully.
   * <p>
   * This is equivalent to <code>android.net.Uri.Builder</code>, which is not
   * present in our JUnit 4 tests.
   *
   * @param path fragment.
   * @param queryParameters list of key/value query parameter pairs.  Must be even length!
   * @return <code>BaseResource<instance>
   * @throws URISyntaxException
   * @throws UnsupportedEncodingException
   */
  protected BaseResource getBaseResource(String path, String... queryParameters) throws URISyntaxException, UnsupportedEncodingException {
    final StringBuilder sb = new StringBuilder(serverURI);
    sb.append(path);
    if (queryParameters != null) {
      int i = 0;
      while (i < queryParameters.length) {
        sb.append(i > 0 ? "&" : "?");
        final String key = queryParameters[i++];
        final String val = queryParameters[i++];
        sb.append(URLEncoder.encode(key, "UTF-8"));
        sb.append("=");
        sb.append(URLEncoder.encode(val, "UTF-8"));
      }
    }
    return new BaseResource(new URI(sb.toString()));
  }

  /**
   * Process a typed value extracted from a successful response (in an
   * endpoint-dependent way).
   */
  public interface RequestDelegate<T> {
    public void handleError(Exception e);
    public void handleFailure(FxAccountClientRemoteException e);
    public void handleSuccess(T result);
  }

  /**
   * Thin container for two cryptographic keys.
   */
  public static class TwoKeys {
    public final byte[] kA;
    public final byte[] wrapkB;
    public TwoKeys(byte[] kA, byte[] wrapkB) {
      this.kA = kA;
      this.wrapkB = wrapkB;
    }
  }

  protected <T> void invokeHandleError(final RequestDelegate<T> delegate, final Exception e) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        delegate.handleError(e);
      }
    });
  }

  enum ResponseType {
    JSON_ARRAY,
    JSON_OBJECT
  }

  /**
   * Translate resource callbacks into request callbacks invoked on the provided
   * executor.
   * <p>
   * Override <code>handleSuccess</code> to parse the body of the resource
   * request and call the request callback. <code>handleSuccess</code> is
   * invoked via the executor, so you don't need to delegate further.
   */
  protected abstract class ResourceDelegate<T> extends BaseResourceDelegate {

    protected void handleSuccess(final int status, HttpResponse response, final ExtendedJSONObject body) throws Exception {
      throw new UnsupportedOperationException();
    }

    protected void handleSuccess(final int status, HttpResponse response, final JSONArray body) throws Exception {
      throw new UnsupportedOperationException();
    }

    protected final RequestDelegate<T> delegate;

    protected final byte[] tokenId;
    protected final byte[] reqHMACKey;
    protected final SkewHandler skewHandler;
    protected final ResponseType responseType;

    /**
     * Create a delegate for an un-authenticated resource.
     */
    public ResourceDelegate(final Resource resource, final RequestDelegate<T> delegate, ResponseType responseType) {
      this(resource, delegate, responseType, null, null);
    }

    /**
     * Create a delegate for a Hawk-authenticated resource.
     * <p>
     * Every Hawk request that encloses an entity (PATCH, POST, and PUT) will
     * include the payload verification hash.
     */
    public ResourceDelegate(final Resource resource, final RequestDelegate<T> delegate, ResponseType responseType, final byte[] tokenId, final byte[] reqHMACKey) {
      super(resource);
      this.delegate = delegate;
      this.reqHMACKey = reqHMACKey;
      this.tokenId = tokenId;
      this.skewHandler = SkewHandler.getSkewHandlerForResource(resource);
      this.responseType = responseType;
    }

    @Override
    public AuthHeaderProvider getAuthHeaderProvider() {
      if (tokenId != null && reqHMACKey != null) {
        // We always include the payload verification hash for FxA Hawk-authenticated requests.
        final boolean includePayloadVerificationHash = true;
        return new HawkAuthHeaderProvider(Utils.byte2Hex(tokenId), reqHMACKey, includePayloadVerificationHash, skewHandler.getSkewInSeconds());
      }
      return super.getAuthHeaderProvider();
    }

    @Override
    public String getUserAgent() {
      return FirefoxSyncInterModuleReceiver.getUserAgent(); // HACK: See class javadoc for more info.
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      try {
        final int status = validateResponse(response);
        skewHandler.updateSkew(response, now());
        invokeHandleSuccess(status, response);
      } catch (FxAccountClientRemoteException e) {
        if (!skewHandler.updateSkew(response, now())) {
          // If we couldn't update skew, but we got a failure, let's try clearing the skew.
          skewHandler.resetSkew();
        }
        invokeHandleFailure(e);
      }
    }

    protected void invokeHandleFailure(final FxAccountClientRemoteException e) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleFailure(e);
        }
      });
    }

    protected void invokeHandleSuccess(final int status, final HttpResponse response) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            SyncResponse syncResponse = new SyncResponse(response);
            if (responseType == ResponseType.JSON_ARRAY) {
              JSONArray body = syncResponse.jsonArrayBody();
              ResourceDelegate.this.handleSuccess(status, response, body);
            } else {
              ExtendedJSONObject body = syncResponse.jsonObjectBody();
              ResourceDelegate.this.handleSuccess(status, response, body);
            }
          } catch (Exception e) {
            delegate.handleError(e);
          }
        }
      });
    }

    @Override
    public void handleHttpProtocolException(final ClientProtocolException e) {
      invokeHandleError(delegate, e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      invokeHandleError(delegate, e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      invokeHandleError(delegate, e);
    }

    @Override
    public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
      super.addHeaders(request, client);

      // The basics.
      final Locale locale = Locale.getDefault();
      request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, Locales.getLanguageTag(locale));
      request.addHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER);
    }
  }

  protected <T> void post(BaseResource resource, final ExtendedJSONObject requestBody) {
    if (requestBody == null) {
      resource.post((HttpEntity) null);
    } else {
      resource.post(requestBody);
    }
  }

  @SuppressWarnings("static-method")
  public long now() {
    return System.currentTimeMillis();
  }

  /**
   * Intepret a response from the auth server.
   * <p>
   * Throw an appropriate exception on errors; otherwise, return the response's
   * status code.
   *
   * @return response's HTTP status code.
   * @throws FxAccountClientException
   */
  public static int validateResponse(HttpResponse response) throws FxAccountClientRemoteException {
    final int status = response.getStatusLine().getStatusCode();
    if (status == 200) {
      return status;
    }
    int code;
    int errno;
    String error;
    String message;
    String info;
    ExtendedJSONObject body;
    try {
      body = new SyncStorageResponse(response).jsonObjectBody();
      body.throwIfFieldsMissingOrMisTyped(requiredErrorStringFields, String.class);
      body.throwIfFieldsMissingOrMisTyped(requiredErrorLongFields, Long.class);
      code = body.getLong(JSON_KEY_CODE).intValue();
      errno = body.getLong(JSON_KEY_ERRNO).intValue();
      error = body.getString(JSON_KEY_ERROR);
      message = body.getString(JSON_KEY_MESSAGE);
      info = body.getString(JSON_KEY_INFO);
    } catch (Exception e) {
      throw new FxAccountClientMalformedResponseException(response);
    }
    throw new FxAccountClientRemoteException(response, code, errno, error, message, info, body);
  }

  /**
   * Don't call this directly. Use <code>unbundleBody</code> instead.
   */
  protected void unbundleBytes(byte[] bundleBytes, byte[] respHMACKey, byte[] respXORKey, byte[]... rest)
      throws InvalidKeyException, NoSuchAlgorithmException, FxAccountClientException {
    if (bundleBytes.length < 32) {
      throw new IllegalArgumentException("input bundle must include HMAC");
    }
    int len = respXORKey.length;
    if (bundleBytes.length != len + 32) {
      throw new IllegalArgumentException("input bundle and XOR key with HMAC have different lengths");
    }
    int left = len;
    for (byte[] array : rest) {
      left -= array.length;
    }
    if (left != 0) {
      throw new IllegalArgumentException("XOR key and total output arrays have different lengths");
    }

    byte[] ciphertext = new byte[len];
    byte[] HMAC = new byte[32];
    System.arraycopy(bundleBytes, 0, ciphertext, 0, len);
    System.arraycopy(bundleBytes, len, HMAC, 0, 32);

    Mac hmacHasher = HKDF.makeHMACHasher(respHMACKey);
    byte[] computedHMAC = hmacHasher.doFinal(ciphertext);
    if (!Arrays.equals(computedHMAC, HMAC)) {
      throw new FxAccountClientException("Bad message HMAC");
    }

    int offset = 0;
    for (byte[] array : rest) {
      for (int i = 0; i < array.length; i++) {
        array[i] = (byte) (respXORKey[offset + i] ^ ciphertext[offset + i]);
      }
      offset += array.length;
    }
  }

  protected void unbundleBody(ExtendedJSONObject body, byte[] requestKey, byte[] ctxInfo, byte[]... rest) throws Exception {
    int length = 0;
    for (byte[] array : rest) {
      length += array.length;
    }

    if (body == null) {
      throw new FxAccountClientException("body must be non-null");
    }
    String bundle = body.getString("bundle");
    if (bundle == null) {
      throw new FxAccountClientException("bundle must be a non-null string");
    }
    byte[] bundleBytes = Utils.hex2Byte(bundle);

    final byte[] respHMACKey = new byte[32];
    final byte[] respXORKey = new byte[length];
    HKDF.deriveMany(requestKey, new byte[0], ctxInfo, respHMACKey, respXORKey);
    unbundleBytes(bundleBytes, respHMACKey, respXORKey, rest);
  }

  public void keys(byte[] keyFetchToken, final RequestDelegate<TwoKeys> delegate) {
    final byte[] tokenId = new byte[32];
    final byte[] reqHMACKey = new byte[32];
    final byte[] requestKey = new byte[32];
    try {
      HKDF.deriveMany(keyFetchToken, new byte[0], FxAccountUtils.KW("keyFetchToken"), tokenId, reqHMACKey, requestKey);
    } catch (Exception e) {
      invokeHandleError(delegate, e);
      return;
    }

    BaseResource resource;
    try {
      resource = getBaseResource("account/keys");
    } catch (URISyntaxException | UnsupportedEncodingException e) {
      invokeHandleError(delegate, e);
      return;
    }

    resource.delegate = new ResourceDelegate<TwoKeys>(resource, delegate, ResponseType.JSON_OBJECT, tokenId, reqHMACKey) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) throws Exception {
        byte[] kA = new byte[FxAccountUtils.CRYPTO_KEY_LENGTH_BYTES];
        byte[] wrapkB = new byte[FxAccountUtils.CRYPTO_KEY_LENGTH_BYTES];
        unbundleBody(body, requestKey, FxAccountUtils.KW("account/keys"), kA, wrapkB);
        delegate.handleSuccess(new TwoKeys(kA, wrapkB));
      }
    };
    resource.get();
  }

  /**
   * Thin container for account status response.
   */
  public static class AccountStatusResponse {
    public final boolean exists;
    public AccountStatusResponse(boolean exists) {
      this.exists = exists;
    }
  }

  /**
   * Thin container for recovery email status response.
   */
  public static class RecoveryEmailStatusResponse {
    public final String email;
    public final boolean verified;
    public RecoveryEmailStatusResponse(String email, boolean verified) {
      this.email = email;
      this.verified = verified;
    }
  }

  @SuppressWarnings("unchecked")
  public void sign(final byte[] sessionToken, final ExtendedJSONObject publicKey, long durationInMilliseconds, final RequestDelegate<String> delegate) {
    final ExtendedJSONObject body = new ExtendedJSONObject();
    body.put("publicKey", publicKey);
    body.put("duration", durationInMilliseconds);

    final byte[] tokenId = new byte[32];
    final byte[] reqHMACKey = new byte[32];
    try {
      HKDF.deriveMany(sessionToken, new byte[0], FxAccountUtils.KW("sessionToken"), tokenId, reqHMACKey);
    } catch (Exception e) {
      invokeHandleError(delegate, e);
      return;
    }

    BaseResource resource;
    try {
      resource = getBaseResource("certificate/sign");
    } catch (URISyntaxException | UnsupportedEncodingException e) {
      invokeHandleError(delegate, e);
      return;
    }

    resource.delegate = new ResourceDelegate<String>(resource, delegate, ResponseType.JSON_OBJECT, tokenId, reqHMACKey) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) throws Exception {
        String cert = body.getString("cert");
        if (cert == null) {
          delegate.handleError(new FxAccountClientException("cert must be a non-null string"));
          return;
        }
        delegate.handleSuccess(cert);
      }
    };
    post(resource, body);
  }

  protected static final String[] LOGIN_RESPONSE_REQUIRED_STRING_FIELDS = new String[] { JSON_KEY_UID, JSON_KEY_SESSIONTOKEN };
  protected static final String[] LOGIN_RESPONSE_REQUIRED_STRING_FIELDS_KEYS = new String[] { JSON_KEY_UID, JSON_KEY_SESSIONTOKEN, JSON_KEY_KEYFETCHTOKEN, };
  protected static final String[] LOGIN_RESPONSE_REQUIRED_BOOLEAN_FIELDS = new String[] { JSON_KEY_VERIFIED };

  /**
   * Thin container for login response.
   * <p>
   * The <code>remoteEmail</code> field is the email address as normalized by the
   * server, and is <b>not necessarily</b> the email address delivered to the
   * <code>login</code> or <code>create</code> call.
   */
  public static class LoginResponse {
    public final String remoteEmail;
    public final String uid;
    public final byte[] sessionToken;
    public final boolean verified;
    public final byte[] keyFetchToken;

    public LoginResponse(String remoteEmail, String uid, boolean verified, byte[] sessionToken, byte[] keyFetchToken) {
      this.remoteEmail = remoteEmail;
      this.uid = uid;
      this.verified = verified;
      this.sessionToken = sessionToken;
      this.keyFetchToken = keyFetchToken;
    }
  }

}
