/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;


import android.support.annotation.NonNull;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.FirefoxSyncException;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException.FailureReason;
import org.mozilla.util.ThrowableUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

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
        // TODO: maybe we should strip PII in non-debug mode?
        // Here's our Exception handling strategy: GetSync*Commands run in a queue (in SyncClientCommandRunner)
        // and any Exceptions thrown in the queue will cascade to be thrown by the final `Future.get()`. These
        // thrown Exceptions should appear in this function.
        //
        // When the Exceptions are initially thrown, given that they're the best place to know the specific reasons
        // for throwing, we catch them and wrap them in FirefoxSyncGetCollectionExceptions with a FailureReason so
        // that the library user can act on them. However, these Exceptions may get wrapped in subsequent Exceptions
        // (in particular, ExecutionExceptions as they cascade through the queue) so we go through the list of
        // wrapped Exceptions to find our FirefoxSyncGetCollectionException and rewrap it at the top level.
        // We *could* strip the upper Exceptions to clean it up but then we lose a little bit of the history so I
        // opted not to.
        //
        // Notes:
        // - We iterate on the list of wrapped Exceptions from the bottom-most first so that if we accidentally
        // wrap a FirefoxSyncGetCollectionException in a FirefoxSyncGetCollectionException, we use the FailureReason
        // closest to the location it occurred as ithas more information.
        // - The implementation doesn't let us catch all relevant Exceptions and wrap them so sometimes a
        // FirefoxSyncGetCollectionException is not present and we have to analyze the wrapped Exceptions to figure
        // out what went wrong. This *tightly couples us* to the implementation. :( For examples we can't catch,
        // see the implementation below.
        // - When the implementation changes, it's pretty easy for someone to throw a
        // non-FirefoxSyncGetCollectionException. :(
        FailureReason failureReason = null;
        final List<Throwable> causes = ThrowableUtils.getRootCauseToTopThrowable(cause);
        for (final Throwable currentCause : causes) {
            if (currentCause instanceof FirefoxSyncGetCollectionException) {
                failureReason = ((FirefoxSyncGetCollectionException) currentCause).getFailureReason();
            }

            // This is where we become tightly coupled to our implementation (see above). :(
            else if (currentCause instanceof TimeoutException) {
                // AsyncChainableCallable, via makeSync, will throw these if an async call takes too long.
                failureReason = FailureReason.TIMED_OUT;

            } else if (currentCause instanceof TokenServerException.TokenServerInvalidCredentialsException) {
                // todo: really?
                failureReason = FailureReason.ACCOUNT_EXPIRED; // most likely the case though it could be our, or the server's, error.
            } else if (currentCause instanceof TokenServerException) {
                // Honestly, I'm not sure what all the TokenServerExceptions do, but they seem to be server-ish errors.
                failureReason = FailureReason.SERVER_RESPONSE_UNEXPECTED;
            }

            if (failureReason != null) { break; } // Return the first match.
        }

        if (failureReason == null) { /* no matches in list. */ failureReason = FailureReason.UNKNOWN; }
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
