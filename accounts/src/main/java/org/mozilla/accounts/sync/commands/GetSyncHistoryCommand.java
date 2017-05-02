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
import org.mozilla.accounts.sync.callbacks.SyncHistoryCallback;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Gets the history for the associated account from Firefox Sync.
 */
public class GetSyncHistoryCommand extends SyncClientResourceCommand {

    private static final String HISTORY_COLLECTION = "history";

    private final int itemLimit;
    private final SyncHistoryCallback callback;

    public GetSyncHistoryCommand(final int itemLimit, final SyncHistoryCallback callback) {
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
        private final SyncHistoryCallback callback;

        public SyncClientHistoryResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncHistoryCallback callback) {
            super(syncConfig);
            this.callback = callback;
        }

        @Override
        public void handleResponse(final HttpResponse response) {
            Log.d(LOGTAG, response.toString());
            Scanner s = null;
            try {
                s = new Scanner(response.getEntity().getContent()).useDelimiter("\\A");
            } catch (IOException e) {
                e.printStackTrace();
            }
            String result = s.hasNext() ? s.next() : "";
            final JSONArray array;
            final HistoryRecordFactory fact = new HistoryRecordFactory();
            try {
                final KeyBundle bundle = syncConfig.collectionKeys.keyBundleForCollection(HISTORY_COLLECTION);
                array = new JSONArray(result);
                for (int i = 0; i < array.length(); ++i) {
                    final JSONObject obj = array.getJSONObject(i);
                    final Record record = new HistoryRecord(obj.getString("id"));
                    final CryptoRecord crecord = new CryptoRecord(record);
                    crecord.payload = new ExtendedJSONObject(obj.getString("payload"));
                    crecord.setKeyBundle(bundle);
                    crecord.decrypt();
                    final HistoryRecord hrecord = (HistoryRecord) fact.createRecord(crecord);
                    Log.d(LOGTAG, hrecord.histURI);
                }
            } catch (JSONException | NonObjectJSONException | IOException | CryptoException | NoCollectionKeysSetException e) {
                callback.onError(e);
                return;
            }
            callback.onReceive(null);
        }

        @Override public void handleError(final Exception e) { callback.onError(e); }
    }
}
