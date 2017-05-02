/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONException;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientCollectionCommand;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;

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
            try {
                callback.onReceive(responseBodyToRecords(responseBody, HISTORY_COLLECTION, new HistoryRecordFactory()));
            } catch (final NoCollectionKeysSetException | JSONException e) {
                callback.onError(e);
            }
        }
    }
}
