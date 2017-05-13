/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import org.mozilla.sync.sync.BookmarkFolder;
import org.mozilla.sync.sync.HistoryRecord;
import org.mozilla.sync.sync.PasswordRecord;
import org.mozilla.sync.sync.SyncCollectionResult;

import java.util.List;

/**
 * TODO:
 */
public interface FirefoxSyncClient {
    // TODO: bookmarks return type.
    SyncCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxSyncGetCollectionException;
    SyncCollectionResult<BookmarkFolder> getBookmarksWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    SyncCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxSyncGetCollectionException;
    SyncCollectionResult<List<HistoryRecord>> getHistoryWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    SyncCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxSyncGetCollectionException;
    SyncCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    String getEmail(); // TODO: verify with server has not changed; throws exception?
}
