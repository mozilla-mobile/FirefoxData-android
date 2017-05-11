/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONException;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecordFactory;
import org.mozilla.util.IOUtil;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Gets the bookmarks for the associated account from Firefox Sync.
 */
class GetSyncBookmarksCommand extends SyncClientCommands.SyncClientCollectionCommand<BookmarkRecord> {

    private static final String BOOKMARKS_COLLECTION = "bookmarks";

    @Override
    public void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<List<BookmarkRecord>> onComplete) {
        final SyncClientBookmarksResourceDelegate resourceDelegate = new SyncClientBookmarksResourceDelegate(syncConfig, onComplete);
        try {
            makeGetRequestForCollection(syncConfig, BOOKMARKS_COLLECTION, null, resourceDelegate);
        } catch (final URISyntaxException e) {
            onComplete.onError(e);
        }
    }

    private static class SyncClientBookmarksResourceDelegate extends SyncClientBaseResourceDelegate<BookmarkRecord> {
        SyncClientBookmarksResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<List<BookmarkRecord>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            try {
                onComplete.onSuccess(responseBodyToRecords(responseBody, BOOKMARKS_COLLECTION, new BookmarkRecordFactory()));
            } catch (final NoCollectionKeysSetException | JSONException e) {
                onComplete.onError(e);
            }
        }
    }
}
