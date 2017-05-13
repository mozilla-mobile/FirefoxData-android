/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONException;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.util.IOUtil;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gets the history for the associated account from Firefox Sync.
 */
class GetSyncHistoryCommand extends SyncClientCommands.SyncClientCollectionCommand<List<HistoryRecord>> {

    private static final String HISTORY_COLLECTION = "history";

    private final int itemLimit;

    GetSyncHistoryCommand(final int itemLimit) {
        this.itemLimit = itemLimit;
    }

    @Override
    public void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<SyncCollectionResult<List<HistoryRecord>>> onComplete) {
        final SyncClientHistoryResourceDelegate resourceDelegate = new SyncClientHistoryResourceDelegate(syncConfig, onComplete);
        try {
            makeGetRequestForCollection(syncConfig, HISTORY_COLLECTION, getArgs(), resourceDelegate);
        } catch (final URISyntaxException e) {
            onComplete.onError(e);
        }
    }

    private Map<String, String> getArgs() {
        if (itemLimit < 0) return Collections.emptyMap(); // TODO: fix or document.

        final Map<String, String> args = new HashMap<>(1);
        args.put("limit", String.valueOf(itemLimit));
        return args;
    }

    private static class SyncClientHistoryResourceDelegate extends SyncClientBaseResourceDelegate<List<HistoryRecord>> {
        SyncClientHistoryResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<SyncCollectionResult<List<HistoryRecord>>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final List<org.mozilla.gecko.sync.repositories.domain.HistoryRecord> rawRecords;
            try {
                rawRecords = responseBodyToRawRecords(syncConfig, responseBody, HISTORY_COLLECTION, new HistoryRecordFactory());
            } catch (final NoCollectionKeysSetException | JSONException e) {
                onComplete.onError(e);
                return;
            }

            final List<HistoryRecord> resultRecords = rawRecordsToResultRecords(rawRecords);
            onComplete.onSuccess(new SyncCollectionResult<>(resultRecords));
        }

        private List<HistoryRecord> rawRecordsToResultRecords(final List<org.mozilla.gecko.sync.repositories.domain.HistoryRecord> rawRecords) {
            // TODO: Sort.
            // Iterating over these a second time is inefficient (the first time creates the raw records list), but it
            // makes for cleaner code: fix if there are perf issues.
            final ArrayList<HistoryRecord> resultRecords = new ArrayList<>(rawRecords.size());
            for (final org.mozilla.gecko.sync.repositories.domain.HistoryRecord rawRecord : rawRecords) {
                resultRecords.add(new HistoryRecord(rawRecord));
            }
            return resultRecords;
        }
    }
}
