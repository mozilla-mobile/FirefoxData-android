/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.WorkerThread;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.fxa_data.FirefoxDataException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gets the history for the associated account from Firefox Sync.
 */
class FirefoxSyncHistory {

    private static final String HISTORY_COLLECTION = "history";

    private FirefoxSyncHistory() {}

    /**
     * Gets history for the given sync config, returning history with the most-recently visited first.
     *
     * Both the request and the callback will run on the given thread (this is unintuitive: issue #3).
     *
     * @param itemLimit The number of items to fetch. If < 0, all items will be fetched.
     */
    @WorkerThread // network request.
    static void getBlocking(final FirefoxSyncConfig syncConfig, final int itemLimit, final OnSyncComplete<List<HistoryRecord>> onComplete) {
        final SyncHistoryResourceDelegate resourceDelegate = new SyncHistoryResourceDelegate(syncConfig, onComplete);
        try {
            FirefoxSyncUtils.makeGetRequestForCollection(syncConfig, HISTORY_COLLECTION, getArgs(itemLimit), resourceDelegate);
        } catch (final FirefoxDataException e) {
            onComplete.onException(e);
        }
    }

    private static Map<String, String> getArgs(final int itemLimit) {
        final Map<String, String> args = new HashMap<>(1);
        if (itemLimit >= 0) { // Fetch all items if < 0.
            args.put("limit", String.valueOf(itemLimit));
        }
        args.put("sort", "newest"); // sort history in the order users would see in their browser.
        return args;
    }

    private static class SyncHistoryResourceDelegate extends SyncBaseResourceDelegate<List<HistoryRecord>> {
        SyncHistoryResourceDelegate(final FirefoxSyncConfig syncConfig, final OnSyncComplete<List<HistoryRecord>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final List<org.mozilla.gecko.sync.repositories.domain.HistoryRecord> rawRecords;
            try {
                rawRecords = responseBodyToRawRecords(syncConfig, responseBody, HISTORY_COLLECTION, new HistoryRecordFactory());
            } catch (final FirefoxDataException e) {
                onComplete.onException(e);
                return;
            }

            final List<HistoryRecord> resultRecords = rawRecordsToResultRecords(rawRecords);
            onComplete.onSuccess(new DataCollectionResult<>(resultRecords));
        }

        private List<HistoryRecord> rawRecordsToResultRecords(final List<org.mozilla.gecko.sync.repositories.domain.HistoryRecord> rawRecords) {
            // Iterating over these a second time is inefficient (the first time creates the raw records list), but it
            // makes for cleaner code: fix if there are perf issues.
            //
            // We assume the result records are already in the desired sort order.
            final ArrayList<HistoryRecord> resultRecords = new ArrayList<>(rawRecords.size());
            for (final org.mozilla.gecko.sync.repositories.domain.HistoryRecord rawRecord : rawRecords) {
                resultRecords.add(new HistoryRecord(rawRecord));
            }
            return resultRecords;
        }
    }
}
