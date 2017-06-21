/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;


import android.support.annotation.NonNull;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.fxa_data.FirefoxSyncException;
import org.mozilla.fxa_data.impl.FirefoxAccount;
import org.mozilla.fxa_data.impl.IOUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of a FirefoxSyncClient that uses a {@link FirefoxAccount} under the hood.
 */
class FirefoxSyncFirefoxAccountClient implements FirefoxSyncClient {

    private static final long REQUEST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60); // This doesn't work - see usage for details.

    private final FirefoxAccount account;
    private final FirefoxSyncConfig syncConfig;

    FirefoxSyncFirefoxAccountClient(final FirefoxAccount account, final TokenServerToken token, final CollectionKeys collectionKeys) {
        if (account.accountState.getStateLabel() != State.StateLabel.Married) {
            throw new IllegalArgumentException("Expected married account. Instead: " + account.accountState.getStateLabel().toString());
        }

        this.account = account;
        this.syncConfig = new FirefoxSyncConfig(token, collectionKeys);
    }

    @NonNull
    @Override
    public SyncCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxSyncException {
        return getBookmarks(-1);
    }

    @NonNull
    @Override
    public SyncCollectionResult<BookmarkFolder> getBookmarksWithLimit(final int itemLimit) throws FirefoxSyncException {
        return getBookmarks(itemLimit);
    }

    @NonNull
    private SyncCollectionResult<BookmarkFolder> getBookmarks(final int itemLimit) throws FirefoxSyncException {
        return getCollectionSync(new GetCollectionCall<BookmarkFolder>() {
            @Override
            public void getCollectionAsync(final OnSyncComplete<BookmarkFolder> onComplete) {
                FirefoxSyncBookmarks.getBlocking(syncConfig, itemLimit, onComplete);
            }
        });
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxSyncException {
        return getPasswords(-1);
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(final int itemLimit) throws FirefoxSyncException {
        return getPasswords(itemLimit);
    }

    @NonNull
    private SyncCollectionResult<List<PasswordRecord>> getPasswords(final int itemLimit) throws FirefoxSyncException {
        return getCollectionSync(new GetCollectionCall<List<PasswordRecord>>() {
            @Override
            public void getCollectionAsync(final OnSyncComplete<List<PasswordRecord>> onComplete) {
                FirefoxSyncPasswords.getBlocking(syncConfig, itemLimit, onComplete);
            }
        });
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxSyncException {
        return getHistory(-1);
    }

    @NonNull
    @Override
    public SyncCollectionResult<List<HistoryRecord>> getHistoryWithLimit(final int itemLimit) throws FirefoxSyncException {
        return getHistory(itemLimit);
    }

    @NonNull
    private SyncCollectionResult<List<HistoryRecord>> getHistory(final int itemLimit) throws FirefoxSyncException {
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
    private <T> SyncCollectionResult<T> getCollectionSync(final GetCollectionCall<T> getCollectionCall) throws FirefoxSyncException {
        try {
            // The get collection calls are actually blocking but w/ delegates (see issue #3) so `makeSync` will only
            // actually time out if the get collection requests time-out. This code is confusing but it works & I
            // didn't have time to clean it up - please clean it up with #3.
            return IOUtils.makeSync(REQUEST_TIMEOUT_MILLIS, new IOUtils.AsyncCall<SyncCollectionResult<T>>() {
                @Override
                public void initAsyncCall(final IOUtils.OnAsyncCallComplete<SyncCollectionResult<T>> onComplete) {
                    getCollectionCall.getCollectionAsync(new OnSyncComplete<T>() {
                        @Override public void onSuccess(final SyncCollectionResult<T> result) { onComplete.onComplete(result); }
                        @Override public void onException(final FirefoxSyncException e) { onComplete.onException(e); }
                    });
                }
            });
        } catch (final ExecutionException e) {
            throw new FirefoxSyncException("Exception occurred during request.", e);
        } catch (final TimeoutException e) {
            throw new FirefoxSyncException("Request timed out.", e);
        }
    }

    @NonNull
    @Override
    public String getEmail() throws FirefoxSyncException {
        return account.email; // We cache the email but it can change (issue #11).
    }

    private interface GetCollectionCall<T> {
        void getCollectionAsync(final OnSyncComplete<T> onComplete);
    }
}
