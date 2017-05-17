/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.support.annotation.NonNull;
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
import org.mozilla.sync.impl.FirefoxAccountUtils;
import org.mozilla.sync.impl.FirefoxSyncRequestUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.mozilla.sync.impl.FirefoxAccountShared.LOGTAG;

/**
 * TODO:
 */
class FirefoxSyncCryptoKeysAccessor {

    private static final String CRYPTO_COLLECTION = "crypto";
    private static final String KEYS_ID = "keys";

    private FirefoxSyncCryptoKeysAccessor() {}

    interface CollectionKeysCallback { // todo
        void onKeysReceived(CollectionKeys collectionKeys);
        void onException(Exception e);
    }

    static void get(@NonNull final FirefoxAccount marriedAccount, @NonNull final TokenServerToken token, @NonNull final CollectionKeysCallback onComplete) {
        final SyncStorageRecordRequest request;
        try {
            request = new SyncStorageRecordRequest(FirefoxSyncRequestUtils.getCollectionURI(token, CRYPTO_COLLECTION, KEYS_ID, null));
        } catch (final URISyntaxException e) {
            onComplete.onException(e);
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
                    onComplete.onException(e);
                    return;
                }

                // TODO: persist keys: see EnsureCrypto5KeysStage.
                onComplete.onKeysReceived(keys);
            }

            @Override
            public void handleRequestFailure(final SyncStorageResponse response) {
                try {
                    onComplete.onException(new Exception("Failed to retrieve crypto keys: " + response.getErrorMessage()));
                } catch (final IOException e) {
                    onComplete.onException(new Exception("Failed to retrieve crypto keys & its error", e));
                }
            }
            @Override public void handleRequestError(final Exception ex) { onComplete.onException(ex); }

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
