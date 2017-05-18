/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.support.annotation.NonNull;
import org.mozilla.sync.sync.BookmarkFolder;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException;
import org.mozilla.sync.sync.HistoryRecord;
import org.mozilla.sync.sync.PasswordRecord;
import org.mozilla.sync.sync.SyncCollectionResult;

import java.util.List;

/**
 * TODO: doc methods.
 */
public interface FirefoxSyncClient {

    @NonNull SyncCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxSyncGetCollectionException;
    @NonNull SyncCollectionResult<BookmarkFolder> getBookmarksWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull SyncCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxSyncGetCollectionException;
    @NonNull SyncCollectionResult<List<HistoryRecord>> getHistoryWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull SyncCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxSyncGetCollectionException;
    @NonNull SyncCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull String getEmail() throws FirefoxSyncException; // TODO: verify with server has not changed; throws exception?
}
