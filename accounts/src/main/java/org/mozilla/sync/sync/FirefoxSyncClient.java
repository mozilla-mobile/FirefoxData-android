/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import org.mozilla.sync.FirefoxSyncException;

import java.util.List;

/**
 * Fro doc: These methods should all time out.
 */
public interface FirefoxSyncClient {

    @NonNull @WorkerThread SyncCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxSyncGetCollectionException; // TODO: maybe throw typed exceptions instead of FailureReason.
    @NonNull @WorkerThread SyncCollectionResult<BookmarkFolder> getBookmarksWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull @WorkerThread SyncCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxSyncGetCollectionException;
    @NonNull @WorkerThread SyncCollectionResult<List<HistoryRecord>> getHistoryWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    @NonNull @WorkerThread SyncCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxSyncGetCollectionException;
    @NonNull @WorkerThread SyncCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    // can change, should not be used to identify user.
    @NonNull String getEmail() throws FirefoxSyncException;
}
