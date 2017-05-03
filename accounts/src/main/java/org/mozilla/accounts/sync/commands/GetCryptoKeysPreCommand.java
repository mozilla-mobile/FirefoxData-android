/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.util.Log;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.FirefoxAccountSyncUtils;
import org.mozilla.accounts.sync.commands.SyncClientCommands.OnAsyncPreCommandComplete;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientAsyncPreCommand;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.domain.RecordParseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.mozilla.accounts.FirefoxAccountShared.LOGTAG;

/** A command to get the crypto keys necessary to begin a sync. */
public class GetCryptoKeysPreCommand extends SyncClientAsyncPreCommand {
    private static final String CRYPTO_COLLECTION = "crypto";
    private static final String KEYS_ID = "keys";

    @Override
    void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final OnAsyncPreCommandComplete onComplete) throws Exception {
        if (syncConfig.token == null) {
            onComplete.onException(new IllegalArgumentException("syncConfig.token unexpectedly null."));
            return;
        }

        final SyncStorageRecordRequest request = new SyncStorageRecordRequest(
                FirefoxAccountSyncUtils.getCollectionURI(syncConfig.token, CRYPTO_COLLECTION, KEYS_ID, null));
        request.delegate = new SyncStorageRequestDelegate() {
            @Override
            public void handleRequestSuccess(final SyncStorageResponse response) {
                final CollectionKeys keys = new CollectionKeys();
                final ExtendedJSONObject body;
                try {
                    body = response.jsonObjectBody();
                    keys.setKeyPairsFromWBO(CryptoRecord.fromJSONRecord(body), syncConfig.getSyncKeyBundle());
                } catch (final IOException | NonObjectJSONException | CryptoException | RecordParseException | NoSuchAlgorithmException | InvalidKeyException e) {
                    onComplete.onException(e);
                    return;
                }

                // TODO: persist keys: see EnsureCrypto5KeysStage.
                onComplete.onSuccess(new FirefoxAccountSyncConfig(syncConfig.contextWeakReference, syncConfig.account,
                        syncConfig.networkExecutor, syncConfig.token, keys));
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
                    return FirefoxAccountSyncUtils.getAuthHeaderProvider(syncConfig.token);
                } catch (UnsupportedEncodingException | URISyntaxException e) {
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
}
