/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import org.mozilla.sync.sync.BookmarkFolder;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException;
import org.mozilla.sync.sync.HistoryRecord;
import org.mozilla.sync.sync.PasswordRecord;
import org.mozilla.sync.sync.SyncCollectionResult;

import java.util.List;

/**
 * TODO: doc methods.
 * These methods should all time out.
 */
public interface FirefoxSyncClient {

    @NonNull @WorkerThread SyncCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxSyncGetCollectionException;
    @NonNull @WorkerThread SyncCollectionResult<BookmarkFolder> getBookmarksWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull @WorkerThread SyncCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxSyncGetCollectionException;
    @NonNull @WorkerThread SyncCollectionResult<List<HistoryRecord>> getHistoryWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull @WorkerThread SyncCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxSyncGetCollectionException;
    @NonNull @WorkerThread SyncCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull String getEmail() throws FirefoxSyncException; // TODO: verify with server has not changed; throws exception?
}
