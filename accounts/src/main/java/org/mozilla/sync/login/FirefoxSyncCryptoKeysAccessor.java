/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.domain.RecordParseException;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxSyncAssertionException;
import org.mozilla.sync.impl.FirefoxSyncRequestUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import static org.mozilla.sync.impl.FirefoxSyncShared.LOGTAG;

/** A container for static functions to get the crypto keys for a Firefox account. */
class FirefoxSyncCryptoKeysAccessor {

    private static final String CRYPTO_COLLECTION = "crypto";
    private static final String KEYS_ID = "keys";

    private FirefoxSyncCryptoKeysAccessor() {}

    /** A callback for a crypto keys request. */
    interface CollectionKeysCallback {
        void onKeysReceived(CollectionKeys collectionKeys);

        /**
         * Called when the user does not have collection keys on the server. This can happen when the user has a brand
         * new account and has not uploaded any sync data yet.
         */
        void onKeysDoNotExist();

        /** Called when the server response denies us the crypto keys, or the response is invalid. */
        void onRequestFailure(Exception e);
        /** Called when we're unable to get a response from the server. */
        void onError(Exception e);
    }

    /**
     * Gets the crypto keys for the given account & sync token.
     *
     * Both the request & the callback occur on the calling thread (this is unintuitive: issue #3).
     *
     * {@code onComplete}'s {@link CollectionKeysCallback#onError(Exception)} will be passed a
     * {@link FirefoxSyncAssertionException} in the event that some assertion we make fails.
     */
    @WorkerThread // network request.
    static void getBlocking(@NonNull final FirefoxAccount marriedAccount, @NonNull final TokenServerToken token, @NonNull final CollectionKeysCallback onComplete) {
        // If the "crypto" collection does not exist, the crypto keys request will 404 and fail. We'd like to actually
        // know why the request failed so we first ensure the "crypto" collection exists.
        FirefoxSyncCollectionInfoAccessor.getBlocking(token, new FirefoxSyncCollectionInfoAccessor.CollectionInfoCallback() {
            @Override
            public void onSuccess(final Collection<String> existingCollectionNames) {
                if (existingCollectionNames.contains("crypto")) {
                    makeCryptoKeysRequest(marriedAccount, token, onComplete);
                } else {
                    onComplete.onKeysDoNotExist();
                }
            }

            @Override public void onRequestFailure(final Exception e) { onComplete.onRequestFailure(e); }
            @Override public void onError(final Exception e) { onComplete.onError(e); }
        });
    }

    private static void makeCryptoKeysRequest(final FirefoxAccount marriedAccount, final TokenServerToken token, final CollectionKeysCallback onComplete) {
        final SyncStorageRecordRequest request;
        try {
            request = new SyncStorageRecordRequest(FirefoxSyncRequestUtils.getCollectionURI(token, CRYPTO_COLLECTION, KEYS_ID, null));
        } catch (final URISyntaxException e) {
            onComplete.onError(new FirefoxSyncAssertionException("Could not create crypto keys request URI", e));
            return;
        }

        request.delegate = new SyncStorageRequestDelegate() {
            @Override
            public void handleRequestSuccess(final SyncStorageResponse response) {
                final CollectionKeys keys = new CollectionKeys();
                final ExtendedJSONObject body;
                try {
                    body = response.jsonObjectBody();
                    keys.setKeyPairsFromWBO(CryptoRecord.fromJSONRecord(body), getSyncKeyBundle(marriedAccount));
                } catch (final IOException | NonObjectJSONException | CryptoException | RecordParseException | NoSuchAlgorithmException | InvalidKeyException e) {
                    onComplete.onRequestFailure(e);
                    return;
                }

                // TODO: persist keys: see EnsureCrypto5KeysStage.
                onComplete.onKeysReceived(keys);
            }

            @Override
            public void handleRequestFailure(final SyncStorageResponse response) {
                try {
                    onComplete.onRequestFailure(new Exception("Failed to retrieve crypto keys: " + response.getErrorMessage()));
                } catch (final IOException e) {
                    onComplete.onRequestFailure(new Exception("Failed to retrieve crypto keys & its error", e));
                }
            }
            @Override public void handleRequestError(final Exception ex) { onComplete.onError(ex); }

            @Override
            public AuthHeaderProvider getAuthHeaderProvider() {
                try {
                    return FirefoxSyncRequestUtils.getAuthHeaderProvider(token);
                } catch (final UnsupportedEncodingException | URISyntaxException e) {
                    Log.e(LOGTAG, "getAuthHeaderProvider: unable to get auth header.");
                    return null; // Oh well - we'll make the request we expect to fail and handle the failed request.
                }
            }

            @Override
            public String ifUnmodifiedSince() {
                return null; // This is what EnsureCrypto5KeysStage.ifUnmodifiedSince returns! TODO: do something here?
            }
        };
        request.get();
    }

    private static KeyBundle getSyncKeyBundle(final FirefoxAccount marriedAccount) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        final Married married = FirefoxAccountUtils.getMarried(marriedAccount.accountState);
        return married.getSyncKeyBundle();
    }
}
