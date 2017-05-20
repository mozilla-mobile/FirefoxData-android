/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;


import android.support.annotation.NonNull;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.FirefoxSyncException;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException.FailureReason;
import org.mozilla.util.IOUtil;
import org.mozilla.util.ThrowableUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * TODO:
 */
class FirefoxSyncFirefoxAccountClient implements FirefoxSyncClient {

    private static final long REQUEST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60); // This doesn't work - see usage for details.

    private final FirefoxAccount account;
    private final FirefoxSyncConfig syncConfig;

    FirefoxSyncFirefoxAccountClient(final FirefoxAccount account, final TokenServerToken token, final CollectionKeys collectionKeys) {
        // todo: assert logged in?
        this.account = account;
        // TODO: use shared executor? How do they stop/get GC'd?
        this.syncConfig = new FirefoxSyncConfig(account, Executors.newSingleThreadExecutor(), token, collectionKeys);
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
        return getCollectionSync(new GetCollectionCall<BookmarkFolder>() {
            @Override
            public void getCollectionAsync(final OnSyncComplete<BookmarkFolder> onComplete) {
                FirefoxSyncBookmarks.getBlocking(syncConfig, itemLimit, onComplete);
            }
        });
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
        return getCollectionSync(new GetCollectionCall<List<PasswordRecord>>() {
            @Override
            public void getCollectionAsync(final OnSyncComplete<List<PasswordRecord>> onComplete) {
                FirefoxSyncPasswords.getBlocking(syncConfig, itemLimit, onComplete);
            }
        });
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
        return getCollectionSync(new GetCollectionCall<List<HistoryRecord>>() {
            @Override
            public void getCollectionAsync(final OnSyncComplete<List<HistoryRecord>> onComplete) {
                FirefoxSyncHistory.getBlocking(syncConfig, itemLimit, onComplete);
            }
        });
    }

    /**
     * Convenience method to share the code to turn the async get collection calls into synchronous calls & handle errors.
     *
     * At the time of writing (5/18/17), the calls using this method have their time out duration specified from
     * the {@link SyncBaseResourceDelegate#connectionTimeout()} underlying the requests.
     */
    private <T> SyncCollectionResult<T> getCollectionSync(final GetCollectionCall<T> getCollectionCall) throws FirefoxSyncGetCollectionException {
        try {
            // The get collection calls are actually blocking but w/ delegates (see issue #3) so `makeSync` will only
            // actually time out if the get collection requests time-out. This code is confusing but it works & I
            // didn't have time to clean it up - please clean it up with #3.
            return IOUtil.makeSync(REQUEST_TIMEOUT_MILLIS, new IOUtil.AsyncCall<SyncCollectionResult<T>>() {
                @Override
                public void initAsyncCall(final IOUtil.OnAsyncCallComplete<SyncCollectionResult<T>> onComplete) {
                    getCollectionCall.getCollectionAsync(new OnSyncComplete<T>() {
                        @Override public void onSuccess(final SyncCollectionResult<T> result) { onComplete.onComplete(result); }
                        @Override public void onException(final FirefoxSyncGetCollectionException e) { onComplete.onException(e); }
                    });
                }
            });
        } catch (final ExecutionException e) {
            throw newGetCollectionException(e);
        } catch (final TimeoutException e) {
            throw new FirefoxSyncGetCollectionException(e, FailureReason.NETWORK_ERROR);
        }
    }

    /** Creates the appropriate exception, in particular identifying its FailureReason, from the given cause. */
    private static FirefoxSyncGetCollectionException newGetCollectionException(final ExecutionException cause) {
        // TODO: maybe we should strip PII in non-debug mode?
        // We want to throw a FirefoxSyncGetCollectionException to the user of the lib but those
        // FirefoxSyncGetCollectionExceptions thrown by the get collection calls are wrapped in ExecutionExceptions
        // because we make them sync. As such, we have to dig through the Exception's causes to return a
        // GetCollectionException with the same FailureReason as the initially thrown Exception. We wrap the given
        // Exception, rather than returning the first exception, in order to maintain the Exception history.
        //
        // Note: We iterate on the list of wrapped Exceptions from the bottom-most first so that if we accidentally
        // wrap a FirefoxSyncGetCollectionException in a FirefoxSyncGetCollectionException, we use the FailureReason
        // closest to the location it occurred as it has more information.
        FailureReason failureReason = null;
        final List<Throwable> causes = ThrowableUtils.getRootCauseToTopThrowable(cause);
        for (final Throwable currentCause : causes) {
            if (currentCause instanceof FirefoxSyncGetCollectionException) {
                failureReason = ((FirefoxSyncGetCollectionException) currentCause).getFailureReason();
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

    private interface GetCollectionCall<T> {
        void getCollectionAsync(final OnSyncComplete<T> onComplete);
    }
}
