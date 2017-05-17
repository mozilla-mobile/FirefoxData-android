/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecordFactory;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets the Passwords associated with the Firefox Account from Sync.
 */
/*
class GetSyncPasswordsCommand extends SyncClientCommands.SyncClientCollectionCommand<List<PasswordRecord>> {

    private static final String PASSWORDS_COLLECTION = "passwords";

    @Override
    public void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final SyncClientCommands.SyncOnAsyncCallComplete<SyncCollectionResult<List<PasswordRecord>>> onComplete) {
        final SyncClientPasswordsResourceDelegate resourceDelegate = new SyncClientPasswordsResourceDelegate(syncConfig, onComplete);
        try {
            makeGetRequestForCollection(syncConfig, PASSWORDS_COLLECTION, null, resourceDelegate);
        } catch (final FirefoxSyncGetCollectionException e) {
            onComplete.onException(e);
        }
    }

    private static class SyncClientPasswordsResourceDelegate extends SyncClientBaseResourceDelegate<List<PasswordRecord>> {
        private SyncClientPasswordsResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncClientCommands.SyncOnAsyncCallComplete<SyncCollectionResult<List<PasswordRecord>>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final List<org.mozilla.gecko.sync.repositories.domain.PasswordRecord> rawRecords;
            try {
                rawRecords = responseBodyToRawRecords(syncConfig, responseBody, PASSWORDS_COLLECTION, new PasswordRecordFactory());
            } catch (final FirefoxSyncGetCollectionException e) {
                onComplete.onException(e);
                return;
            }

            final List<PasswordRecord> resultRecords = rawRecordsToResultRecords(rawRecords);
            onComplete.onComplete(new SyncCollectionResult<>(resultRecords));
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
*/
