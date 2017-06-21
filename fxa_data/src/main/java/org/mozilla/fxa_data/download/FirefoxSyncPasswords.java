/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.WorkerThread;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecordFactory;
import org.mozilla.fxa_data.FirefoxSyncException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gets the Passwords associated with the Firefox Account from Sync.
 */
class FirefoxSyncPasswords {

    private static final String PASSWORDS_COLLECTION = "passwords";

    private FirefoxSyncPasswords() {}

    /**
     * Gets the passwords for the given account.
     *
     * Both the request and callback occur on the calling thread (this is unintuitive: issue #3).
     *
     * @param itemLimit The number of items to fetch. If < 0, returns all items.
     */
    @WorkerThread // network request.
    static void getBlocking(final FirefoxSyncConfig syncConfig, final int itemLimit, final OnSyncComplete<List<PasswordRecord>> onComplete) {
        final SyncPasswordsResourceDelegate resourceDelegate = new SyncPasswordsResourceDelegate(syncConfig, onComplete);
        try {
            FirefoxSyncUtils.makeGetRequestForCollection(syncConfig, PASSWORDS_COLLECTION, getArgs(itemLimit), resourceDelegate);
        } catch (final FirefoxSyncException e) {
            onComplete.onException(e);
        }
    }

    private static Map<String, String> getArgs(final int itemLimit) {
        if (itemLimit < 0) { return null; } // Fetch all items if < 0.

        final Map<String, String> args = new HashMap<>(1);
        args.put("limit", String.valueOf(itemLimit));
        return args;
    }

    private static class SyncPasswordsResourceDelegate extends SyncBaseResourceDelegate<List<PasswordRecord>> {
        private SyncPasswordsResourceDelegate(final FirefoxSyncConfig syncConfig, final OnSyncComplete<List<PasswordRecord>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final List<org.mozilla.gecko.sync.repositories.domain.PasswordRecord> rawRecords;
            try {
                rawRecords = responseBodyToRawRecords(syncConfig, responseBody, PASSWORDS_COLLECTION, new PasswordRecordFactory());
            } catch (final FirefoxSyncException e) {
                onComplete.onException(e);
                return;
            }

            final List<PasswordRecord> resultRecords = rawRecordsToResultRecords(rawRecords);
            onComplete.onSuccess(new SyncCollectionResult<>(resultRecords));
        }

        private List<PasswordRecord> rawRecordsToResultRecords(final List<org.mozilla.gecko.sync.repositories.domain.PasswordRecord> rawRecords) {
            // Iterating over these a second time is inefficient (the first time creates the raw records list), but it
            // makes for cleaner code: fix if there are perf issues.
            final ArrayList<PasswordRecord> resultRecords = new ArrayList<>(rawRecords.size());
            for (final org.mozilla.gecko.sync.repositories.domain.PasswordRecord rawRecord : rawRecords) {
                resultRecords.add(new PasswordRecord(rawRecord));
            }
            return resultRecords;
        }
    }
}
