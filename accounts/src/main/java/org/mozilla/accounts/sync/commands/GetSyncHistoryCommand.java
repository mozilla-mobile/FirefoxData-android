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
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Gets the history for the associated account from Firefox Sync.
 */
public class GetSyncHistoryCommand extends SyncClientCollectionCommand<HistoryRecord> {

    private static final String HISTORY_COLLECTION = "history";

    private final int itemLimit;

    public GetSyncHistoryCommand(final int itemLimit, final SyncCollectionCallback<HistoryRecord> callback) {
        super(callback);
        this.itemLimit = itemLimit;
    }

    @Override
    public void callWithCallback(final FirefoxAccountSyncConfig syncConfig) throws Exception {
        final SyncClientHistoryResourceDelegate resourceDelegate = new SyncClientHistoryResourceDelegate(syncConfig, callback);
        makeGetRequestForCollection(syncConfig, HISTORY_COLLECTION, getArgs(), resourceDelegate);
    }

    private Map<String, String> getArgs() {
        final Map<String, String> args = new HashMap<>(1);
        args.put("limit", String.valueOf(itemLimit));
        return args;
    }

    private static class SyncClientHistoryResourceDelegate extends SyncClientBaseResourceDelegate<HistoryRecord> {
        public SyncClientHistoryResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncCollectionCallback<HistoryRecord> callback) {
            super(syncConfig, callback);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final HistoryRecordFactory recordFactory = new HistoryRecordFactory();
            final KeyBundle keyBundle;
            final JSONArray recordArray;
            try {
                keyBundle = syncConfig.collectionKeys.keyBundleForCollection(HISTORY_COLLECTION);
                recordArray = new JSONArray(responseBody);
            } catch (final NoCollectionKeysSetException | JSONException e) {
                callback.onError(e);
                return;
            }

            final ArrayList<HistoryRecord> receivedRecords = new ArrayList<>(recordArray.length());
            for (int i = 0; i < recordArray.length(); ++i) {
                try {
                    final JSONObject jsonRecord = recordArray.getJSONObject(i);
                    final HistoryRecord historyRecord = getAndDecryptRecord(recordFactory, keyBundle, jsonRecord);
                    receivedRecords.add(historyRecord);
                } catch (final IOException | JSONException | NonObjectJSONException | CryptoException e) {
                    Log.w(LOGTAG, "Unable to decrypt record", e); // Let's not log to avoid leaking user data.
                }
            }
            callback.onReceive(receivedRecords);
        }

        private HistoryRecord getAndDecryptRecord(final HistoryRecordFactory recordFactory, final KeyBundle keyBundle,
                final JSONObject json) throws NonObjectJSONException, IOException, CryptoException, JSONException {
            final Record recordToWrap = new HistoryRecord(json.getString("id"));
            final CryptoRecord cryptoRecord = new CryptoRecord(recordToWrap);
            cryptoRecord.payload = new ExtendedJSONObject(json.getString("payload"));
            cryptoRecord.setKeyBundle(keyBundle);
            cryptoRecord.decrypt();
            return (HistoryRecord) recordFactory.createRecord(cryptoRecord);
        }
    }
}
