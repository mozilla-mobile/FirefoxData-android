/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientCollectionCommand;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Gets the Passwords associated with the Firefox Account.
 */
public class GetSyncPasswordsCommand extends SyncClientCollectionCommand<PasswordRecord> {

    private static final String PASSWORDS_COLLECTION = "passwords"; // TODO: what am I?

    public GetSyncPasswordsCommand(final SyncCollectionCallback<PasswordRecord> callback) {
        super(callback);
    }

    @Override
    public void callWithCallback(final FirefoxAccountSyncConfig syncConfig) throws Exception {
        final SyncClientPasswordsResourceDelegate resourceDelegate = new SyncClientPasswordsResourceDelegate(syncConfig, callback);
        makeGetRequestForCollection(syncConfig, PASSWORDS_COLLECTION, null, resourceDelegate);
    }

    private static class SyncClientPasswordsResourceDelegate extends SyncClientBaseResourceDelegate<PasswordRecord> {
        private SyncClientPasswordsResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncCollectionCallback<PasswordRecord> callback) {
            super(syncConfig, callback);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final PasswordRecordFactory recordFactory = new PasswordRecordFactory();
            final KeyBundle keyBundle;
            final JSONArray recordArray;
            try {
                keyBundle = syncConfig.collectionKeys.keyBundleForCollection(PASSWORDS_COLLECTION);
                recordArray = new JSONArray(responseBody);
            } catch (final NoCollectionKeysSetException | JSONException e) {
                callback.onError(e);
                return;
            }

            final ArrayList<PasswordRecord> receivedRecords = new ArrayList<>(recordArray.length());
            for (int i = 0; i < recordArray.length(); ++i) {
                try {
                    final JSONObject jsonRecord = recordArray.getJSONObject(i);
                    final PasswordRecord historyRecord = getAndDecryptRecord(recordFactory, keyBundle, jsonRecord);
                    receivedRecords.add(historyRecord);
                } catch (final IOException | JSONException | NonObjectJSONException | CryptoException e) {
                    Log.w(LOGTAG, "Unable to decrypt record", e); // Let's not log to avoid leaking user data.
                }
            }
            callback.onReceive(receivedRecords);
        }

        private PasswordRecord getAndDecryptRecord(final PasswordRecordFactory recordFactory, final KeyBundle keyBundle,
                final JSONObject json) throws NonObjectJSONException, IOException, CryptoException, JSONException {
            final Record recordToWrap = new PasswordRecord(json.getString("id"));
            final CryptoRecord cryptoRecord = new CryptoRecord(recordToWrap);
            cryptoRecord.payload = new ExtendedJSONObject(json.getString("payload"));
            cryptoRecord.setKeyBundle(keyBundle);
            cryptoRecord.decrypt();
            return (PasswordRecord) recordFactory.createRecord(cryptoRecord);
        }
    }
}
