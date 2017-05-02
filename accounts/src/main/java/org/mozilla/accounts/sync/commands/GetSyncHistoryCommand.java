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
import org.mozilla.accounts.sync.FirefoxAccountSyncUtils;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientResourceCommand;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Gets the history for the associated account from Firefox Sync.
 */
public class GetSyncHistoryCommand extends SyncClientResourceCommand {

    private static final String HISTORY_COLLECTION = "history";

    private final int itemLimit;
    private final SyncRecordCallback<HistoryRecord> callback;

    public GetSyncHistoryCommand(final int itemLimit, final SyncRecordCallback<HistoryRecord> callback) {
        super(callback);
        this.itemLimit = itemLimit;
        this.callback = callback;
    }

    @Override
    public void callWithCallback(final FirefoxAccountSyncConfig syncConfig) throws Exception {
        // TODO: some code is shared.
        final SyncClientHistoryResourceDelegate resourceDelegate = new SyncClientHistoryResourceDelegate(syncConfig, callback);
        final URI uri = FirefoxAccountSyncUtils.getCollectionURI(syncConfig.token, HISTORY_COLLECTION, null, getArgs());
        final BaseResource resource = new BaseResource(uri);
        resource.delegate = resourceDelegate;
        resource.get();
    }

    private Map<String, String> getArgs() {
        final Map<String, String> args = new HashMap<>(2);
        args.put("full", "1"); // get full data, not just IDs.
        args.put("limit", String.valueOf(itemLimit));
        return args;
    }

    private static class SyncClientHistoryResourceDelegate extends SyncClientBaseResourceDelegate {
        private final SyncRecordCallback<HistoryRecord> callback;

        public SyncClientHistoryResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncRecordCallback<HistoryRecord> callback) {
            super(syncConfig, callback);
            this.callback = callback;
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

        @Override public void handleError(final Exception e) { callback.onError(e); }
    }
}
