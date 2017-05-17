/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mozilla.sync.sync.SyncClientCommands.SyncClientCollectionCommand.makeGetRequestForCollection;

/**
 * Gets the history for the associated account from Firefox Sync.
 */
class FirefoxSyncHistory {

    private static final String HISTORY_COLLECTION = "history";

    private FirefoxSyncHistory() {}

    static void get(final FirefoxAccountSyncConfig syncConfig, final int itemLimit, final OnSyncComplete<List<HistoryRecord>> onComplete) {
        final SyncHistoryResourceDelegate resourceDelegate = new SyncHistoryResourceDelegate(syncConfig, onComplete);
        try {
            makeGetRequestForCollection(syncConfig, HISTORY_COLLECTION, getArgs(itemLimit), resourceDelegate);
        } catch (final FirefoxSyncGetCollectionException e) {
            onComplete.onException(e);
        }
    }

    private static Map<String, String> getArgs(final int itemLimit) {
        if (itemLimit < 0) return Collections.emptyMap(); // TODO: fix or document.

        final Map<String, String> args = new HashMap<>(1);
        args.put("limit", String.valueOf(itemLimit));
        return args;
    }

    private static class SyncHistoryResourceDelegate extends SyncClientBaseResourceDelegate<List<HistoryRecord>> {
        SyncHistoryResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final OnSyncComplete<List<HistoryRecord>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final List<org.mozilla.gecko.sync.repositories.domain.HistoryRecord> rawRecords;
            try {
                rawRecords = responseBodyToRawRecords(syncConfig, responseBody, HISTORY_COLLECTION, new HistoryRecordFactory());
            } catch (final FirefoxSyncGetCollectionException e) {
                onComplete.onException(e);
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
