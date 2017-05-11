/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONException;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecordFactory;
import org.mozilla.util.IOUtil;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Gets the Passwords associated with the Firefox Account from Sync.
 */
class GetSyncPasswordsCommand extends SyncClientCommands.SyncClientCollectionCommand<PasswordRecord> {

    private static final String PASSWORDS_COLLECTION = "passwords";

    @Override
    public void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<List<PasswordRecord>> onComplete) {
        final SyncClientPasswordsResourceDelegate resourceDelegate = new SyncClientPasswordsResourceDelegate(syncConfig, onComplete);
        try {
            makeGetRequestForCollection(syncConfig, PASSWORDS_COLLECTION, null, resourceDelegate);
        } catch (final URISyntaxException e) {
            onComplete.onError(e);
        }
    }

    private static class SyncClientPasswordsResourceDelegate extends SyncClientBaseResourceDelegate<PasswordRecord> {
        private SyncClientPasswordsResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<List<PasswordRecord>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            try {
                onComplete.onSuccess(responseBodyToRecords(responseBody, PASSWORDS_COLLECTION, new PasswordRecordFactory()));
            } catch (final NoCollectionKeysSetException | JSONException e) {
                onComplete.onError(e);
            }
        }
    }
}
