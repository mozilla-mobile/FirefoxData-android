/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONException;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientCollectionCommand;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecordFactory;

/**
 * Gets the Passwords associated with the Firefox Account from Sync.
 */
public class GetSyncPasswordsCommand extends SyncClientCollectionCommand<PasswordRecord> {

    private static final String PASSWORDS_COLLECTION = "passwords";

    public GetSyncPasswordsCommand(final SyncCollectionCallback<PasswordRecord> callback) {
        super(callback);
    }

    @Override
    public void callWithCallback(final FirefoxAccountSyncConfig syncConfig) throws Exception {
        final SyncClientPasswordsResourceDelegate resourceDelegate = new SyncClientPasswordsResourceDelegate(syncConfig, callback);
        makeGetRequestForCollection(syncConfig, PASSWORDS_COLLECTION, null, resourceDelegate);
    }

    private static class SyncClientPasswordsResourceDelegate extends SyncClientBaseResourceDelegate<PasswordRecord> {
        private SyncClientPasswordsResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncCollectionCallback<PasswordRecord> callback) {
            super(syncConfig, callback);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            try {
                callback.onReceive(responseBodyToRecords(responseBody, PASSWORDS_COLLECTION, new PasswordRecordFactory()));
            } catch (final NoCollectionKeysSetException | JSONException e) {
                callback.onError(e);
            }
        }
    }
}
