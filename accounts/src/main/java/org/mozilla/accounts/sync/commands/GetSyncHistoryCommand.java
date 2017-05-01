/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.accounts.sync.FirefoxAccountSyncUtils;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.callbacks.SyncHistoryCallback;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientResourceCommand;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;

import java.io.IOException;
import java.net.URI;
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
        final SyncClientHistoryResourceDelegate resourceDelegate = new SyncClientHistoryResourceDelegate(syncConfig, itemLimit, callback);

        // TODO: set up code is shared.
        final URI storageServerURI = FirefoxAccountSyncUtils.getServerURI(syncConfig.token);
        final URI uri = new URI(storageServerURI.toString() + resourceDelegate.getResourcePath()); // TODO: to util?
        final BaseResource resource = new BaseResource(uri);
        resource.delegate = resourceDelegate;
        resource.get();
    }

    private static class SyncClientHistoryResourceDelegate extends SyncClientBaseResourceDelegate {
        private final int itemLimit;
        private final SyncHistoryCallback callback;

        public SyncClientHistoryResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final int itemLimit,
                final SyncHistoryCallback callback) {
            super(syncConfig);
            this.itemLimit = itemLimit;
            this.callback = callback;
        }

        @Override public void handleError(final Exception e) { callback.onError(e); }

        @Override public String getResourcePath() { return "/storage/history?full=1&limit=" + itemLimit; } // TODO: seems unnecessary.

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
    }
}
