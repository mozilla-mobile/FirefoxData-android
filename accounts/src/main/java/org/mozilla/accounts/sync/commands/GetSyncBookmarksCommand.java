/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONException;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientCollectionCommand;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Gets the bookmarks for the associated account from Firefox Sync.
 */
public class GetSyncBookmarksCommand extends SyncClientCollectionCommand<BookmarkRecord> {

    private static final String BOOKMARKS_COLLECTION = "bookmarks";

    public GetSyncBookmarksCommand(final SyncCollectionCallback<BookmarkRecord> callback) {
        super(callback);
    }

    @Override
    public void callWithCallback(final FirefoxAccountSyncConfig syncConfig) throws Exception {
        final SyncClientBookmarksResourceDelegate resourceDelegate = new SyncClientBookmarksResourceDelegate(syncConfig, callback);
        makeGetRequestForCollection(syncConfig, BOOKMARKS_COLLECTION, null, resourceDelegate);
    }

    private static class SyncClientBookmarksResourceDelegate extends SyncClientBaseResourceDelegate<BookmarkRecord> {
        public SyncClientBookmarksResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final SyncCollectionCallback<BookmarkRecord> callback) {
            super(syncConfig, callback);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            try {
                callback.onReceive(responseBodyToRecords(responseBody, BOOKMARKS_COLLECTION, new BookmarkRecordFactory()));
            } catch (final NoCollectionKeysSetException | JSONException e) {
                callback.onError(e);
            }
        }
    }
}
