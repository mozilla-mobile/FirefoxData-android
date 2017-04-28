/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.mozilla.accounts.FirefoxAccountShared;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.ResourceDelegate;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Base implementation for requests made by {@see org.mozilla.accounts.sync.FirefoxAccountSyncClient}:
 * provides basic configuration and simplifies the error/response handling.
 *
 * {@link #syncConfig} & {@link #authHeaderProvider} should be set by mutation before the request is made.
 */
public abstract class SyncClientBaseResourceDelegate implements ResourceDelegate {
    protected static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    private static final int connectionTimeoutInMillis = 1000 * 30; // Wait 30s for a connection to open.
    private static final int socketTimeoutInMillis = 1000 * 2 * 60; // Wait 2 minutes for data.

    /**
     * The authHeaderProvider returned by {@see #getAuthHeaderProvider}. It's inconvenient to pass this
     * value into the constructor so we allow mutation.
     */
    public AuthHeaderProvider authHeaderProvider = null;

    /**
     * The sync config associated with the account making the request. It's inconvenient to pass this value into
     * the constructor so we allow mutation.
     */
    public SyncConfiguration syncConfig = null;

    /** @return {@code <path>} in {@code <server-storage-uri><path>} */
    public abstract String getResourcePath();

    public abstract void handleError(Exception e);
    public abstract void handleResponse(final HttpResponse response);

    @Override
    public final void handleHttpResponse(final HttpResponse response) {
        if (syncConfig == null) {
            throw new IllegalArgumentException("Expected collectionKeys to be set via mutation before request.");
        }
        // TODO: I don't really understand how these keys work - do we need to null them at some point?
        // TODO: we actually need a separate request to fetch these. fuck.
        syncConfig.setCollectionKeys(syncConfig.persistedCryptoKeys().keys());
        handleResponse(response);
    }

    @Override public String getUserAgent() { return null; } // TODO: return one in Constants?

    // To keep things simple (for now), let's just set them all as errors.
    @Override public void handleHttpProtocolException(final ClientProtocolException e) { handleError(e); }
    @Override public void handleHttpIOException(final IOException e) { handleError(e); }
    @Override public void handleTransportException(final GeneralSecurityException e) { handleError(e); }

    @Override public int connectionTimeout() { return connectionTimeoutInMillis; }
    @Override public int socketTimeout() { return socketTimeoutInMillis; }
    @Override public AuthHeaderProvider getAuthHeaderProvider() { return authHeaderProvider; }

    @Override public void addHeaders(HttpRequestBase request, DefaultHttpClient client) { }
}
