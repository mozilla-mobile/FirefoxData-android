/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;


import android.support.annotation.NonNull;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.FirefoxSyncException;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException.FailureReason;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.util.ThrowableUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TODO:
 */
class FirefoxSyncFirefoxAccountClient implements FirefoxSyncClient {

    private final SyncClientCommandRunner commandRunner = new SyncClientCommandRunner();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(); // TODO: use shared executor? How do they stop/get GC'd?

    private final FirefoxAccount account;

    public FirefoxSyncFirefoxAccountClient(final FirefoxAccount account) {
        // todo: assert logged in?
        this.account = account;
    }

    @NonNull
    @Override
    public SyncCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxSyncGetCollectionException {
        return getBookmarks(-1);
    }

    @NonNull
    @Override
    public SyncCollectionResult<BookmarkFolder> getBookmarksWithLimit(final int itemLimit) throws FirefoxSyncGetCollectionException {
        return getBookmarks(itemLimit);
    }

    @NonNull
    private SyncCollectionResult<BookmarkFolder> getBookmarks(final int itemLimit) throws FirefoxSyncGetCollectionException { // TODO: use itemLimit.
        final Future<SyncCollectionResult<BookmarkFolder>> future = commandRunner.queueAndRunCommand(new GetSyncBookmarksCommand(), getInitialSyncConfig());
        try {
            return future.get(); // todo: timeout.
        } catch (final InterruptedException | ExecutionException e) {
            throw newGetCollectionException(e);
        }
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxSyncGetCollectionException {
        return getPasswords(-1);
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(final int itemLimit) throws FirefoxSyncGetCollectionException {
        return getPasswords(itemLimit);
    }

    @NonNull
    private SyncCollectionResult<List<PasswordRecord>> getPasswords(final int itemLimit) throws FirefoxSyncGetCollectionException { // TODO: use itemLimit.
        final Future<SyncCollectionResult<List<PasswordRecord>>> future = commandRunner.queueAndRunCommand(new GetSyncPasswordsCommand(), getInitialSyncConfig());
        try {
            return future.get(); // todo: timeout.
        } catch (final InterruptedException | ExecutionException e) {
            throw newGetCollectionException(e);
        }
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxSyncGetCollectionException {
        return getHistory(-1);
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<HistoryRecord>> getHistoryWithLimit(final int itemLimit) throws FirefoxSyncGetCollectionException {
        return getHistory(itemLimit);
    }

    @NonNull
    private SyncCollectionResult<List<HistoryRecord>> getHistory(final int itemLimit) throws FirefoxSyncGetCollectionException {
        final Future<SyncCollectionResult<List<HistoryRecord>>> future = commandRunner.queueAndRunCommand(new GetSyncHistoryCommand(itemLimit), getInitialSyncConfig());
        try {
            return future.get(); // todo: timeout.
        } catch (final InterruptedException | ExecutionException e) {
            throw newGetCollectionException(e);
        }
    }

    /** Creates the appropriate exception, in particular identifying its FailureReason, from the given cause. */
    private static FirefoxSyncGetCollectionException newGetCollectionException(final Throwable cause) {
        final FailureReason failureReason;
        final Throwable rootCause = ThrowableUtils.getRootCause(cause);
        if (rootCause instanceof TokenServerException.TokenServerInvalidCredentialsException) {
            failureReason = FailureReason.REQUIRES_LOGIN_PROMPT;
        } else {
            failureReason = FailureReason.UNKNOWN;
        }
        return new FirefoxSyncGetCollectionException(cause, failureReason);
    }

    @NonNull
    @Override
    public String getEmail() throws FirefoxSyncException {
        return account.email; // todo: email/account can get updated.
    }

    private FirefoxAccountSyncConfig getInitialSyncConfig() {
        return new FirefoxAccountSyncConfig(account, networkExecutor, null, null);
    }
}
