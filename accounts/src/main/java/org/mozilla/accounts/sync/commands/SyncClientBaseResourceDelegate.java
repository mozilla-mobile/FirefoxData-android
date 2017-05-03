/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.accounts.FirefoxAccountShared;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.FirefoxAccountSyncUtils;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.util.FileUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for requests made by {@see org.mozilla.accounts.sync.FirefoxAccountSyncClient}:
 * provides basic configuration and simplifies the error/response handling.
 */
public abstract class SyncClientBaseResourceDelegate<R> implements ResourceDelegate {
    protected static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    private static final int connectionTimeoutInMillis = 1000 * 30; // Wait 30s for a connection to open.
    private static final int socketTimeoutInMillis = 1000 * 2 * 60; // Wait 2 minutes for data.

    /** The sync config associated with the request. */
    protected final FirefoxAccountSyncConfig syncConfig;
    protected final SyncCollectionCallback<R> callback;

    public SyncClientBaseResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncCollectionCallback<R> callback) {
        this.syncConfig = syncConfig;
        this.callback = callback;
    }

    public abstract void handleResponse(final HttpResponse response, final String responseBody);

    @Override
    public final void handleHttpResponse(final HttpResponse response) {
        final String responseBody;
        try {
            responseBody = FileUtil.readStringFromInputStreamAndCloseStream(response.getEntity().getContent(), 4096);
        } catch (final IOException e) {
            callback.onError(e);
            return;
        }
        handleResponse(response, responseBody);
    }

    /**
     * Handles any errors that happen in the request process. This can be overridden to have custom behavior; the
     * default implementation just forwards the exception to the callback.
     */
    public void handleError(Exception e) { callback.onError(e); }

    @Override public String getUserAgent() { return null; }

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

    /** Convenience function to turn a request's response body into a list of records of the parametrized type. */
    protected List<R> responseBodyToRecords(final String responseBody, final String collectionName,
            final RecordFactory recordFactory) throws NoCollectionKeysSetException, JSONException {
        final KeyBundle keyBundle = syncConfig.collectionKeys.keyBundleForCollection(collectionName);
        final JSONArray recordArray = new JSONArray(responseBody);

        final ArrayList<R> receivedRecords = new ArrayList<>(recordArray.length());
        for (int i = 0; i < recordArray.length(); ++i) {
            try {
                final JSONObject jsonRecord = recordArray.getJSONObject(i);
                final R record = getAndDecryptRecord(recordFactory, keyBundle, jsonRecord);
                receivedRecords.add(record);
            } catch (final IOException | JSONException | NonObjectJSONException | CryptoException e) {
                Log.w(LOGTAG, "Unable to decrypt record", e); // Let's not log to avoid leaking user data.
            }
        }
        return receivedRecords;
    }

    private R getAndDecryptRecord(final RecordFactory recordFactory, final KeyBundle keyBundle,
            final JSONObject json) throws NonObjectJSONException, IOException, CryptoException, JSONException {
        final Record recordToWrap = new HistoryRecord(json.getString("id")); // Not the most correct but this can be any record since we just init id.
        final CryptoRecord cryptoRecord = new CryptoRecord(recordToWrap);
        cryptoRecord.payload = new ExtendedJSONObject(json.getString("payload"));
        cryptoRecord.setKeyBundle(keyBundle);
        cryptoRecord.decrypt();
        return (R) recordFactory.createRecord(cryptoRecord); // TODO: rm cast. To save time, I didn't generify RecordFactory.
    }
}
