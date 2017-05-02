/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.mozilla.accounts.FirefoxAccountShared;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.FirefoxAccountSyncUtils;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.util.ChainableCallable.ChainableCallableCallback;
import org.mozilla.util.FileUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

/**
 * Base implementation for requests made by {@see org.mozilla.accounts.sync.FirefoxAccountSyncClient}:
 * provides basic configuration and simplifies the error/response handling.
 */
public abstract class SyncClientBaseResourceDelegate implements ResourceDelegate {
    protected static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    private static final int connectionTimeoutInMillis = 1000 * 30; // Wait 30s for a connection to open.
    private static final int socketTimeoutInMillis = 1000 * 2 * 60; // Wait 2 minutes for data.

    /** The sync config associated with the request. */
    protected final FirefoxAccountSyncConfig syncConfig;
    private final ChainableCallableCallback callback;

    public SyncClientBaseResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final ChainableCallableCallback callback) {
        this.syncConfig = syncConfig;
        this.callback = callback;
    }

    public abstract void handleError(Exception e);
    public abstract void handleResponse(final HttpResponse response, final String responseBody);

    @Override public final void handleHttpResponse(final HttpResponse response) {
        final String responseBody;
        try {
            responseBody = FileUtil.readStringFromInputStreamAndCloseStream(response.getEntity().getContent(), 4096);
        } catch (final IOException e) {
            callback.onError(e);
            return;
        }
        handleResponse(response, responseBody);
    }

    @Override public String getUserAgent() { return null; } // TODO: return one in Constants?

    // To keep things simple (for now), let's just set them all as errors.
    @Override public void handleHttpProtocolException(final ClientProtocolException e) { handleError(e); }
    @Override public void handleHttpIOException(final IOException e) { handleError(e); }
    @Override public void handleTransportException(final GeneralSecurityException e) { handleError(e); }

    @Override public int connectionTimeout() { return connectionTimeoutInMillis; }
    @Override public int socketTimeout() { return socketTimeoutInMillis; }
    @Override public AuthHeaderProvider getAuthHeaderProvider() {
        try {
            return FirefoxAccountSyncUtils.getAuthHeaderProvider(syncConfig.token);
        } catch (UnsupportedEncodingException | URISyntaxException e) {
            Log.e(LOGTAG, "getAuthHeaderProvider: unable to get auth header.");
            return null; // Oh well - we'll make the request we expect to fail and handle the failed request.
        }
    }

    @Override public void addHeaders(HttpRequestBase request, DefaultHttpClient client) { }
}
