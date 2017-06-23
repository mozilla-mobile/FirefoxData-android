/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;


import android.support.annotation.NonNull;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.fxa_data.FirefoxDataException;
import org.mozilla.fxa_data.impl.FirefoxAccount;
import org.mozilla.fxa_data.impl.IOUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of a FirefoxDataClient that uses a {@link FirefoxAccount} under the hood.
 */
class FirefoxDataFirefoxAccountClient implements FirefoxDataClient {

    private static final long REQUEST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60); // This doesn't work - see usage for details.

    private final FirefoxAccount account;
    private final FirefoxSyncConfig syncConfig;

    FirefoxDataFirefoxAccountClient(final FirefoxAccount account, final TokenServerToken token, final CollectionKeys collectionKeys) {
        if (account.accountState.getStateLabel() != State.StateLabel.Married) {
            throw new IllegalArgumentException("Expected married account. Instead: " + account.accountState.getStateLabel().toString());
        }

        this.account = account;
        this.syncConfig = new FirefoxSyncConfig(token, collectionKeys);
    }

    @NonNull
    @Override
    public DataCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxDataException {
        return getBookmarks(-1);
    }

    @NonNull
    @Override
    public DataCollectionResult<BookmarkFolder> getBookmarksWithLimit(final int itemLimit) throws FirefoxDataException {
        return getBookmarks(itemLimit);
    }

    @NonNull
    private DataCollectionResult<BookmarkFolder> getBookmarks(final int itemLimit) throws FirefoxDataException {
        return getCollectionSync(new GetCollectionCall<BookmarkFolder>() {
            @Override
            public void getCollectionAsync(final OnSyncComplete<BookmarkFolder> onComplete) {
                FirefoxSyncBookmarks.getBlocking(syncConfig, itemLimit, onComplete);
            }
        });
    }

    @NonNull
    @Override
    public DataCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxDataException {
        return getPasswords(-1);
    }

    @NonNull
    @Override
    public DataCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(final int itemLimit) throws FirefoxDataException {
        return getPasswords(itemLimit);
    }

    @NonNull
    private DataCollectionResult<List<PasswordRecord>> getPasswords(final int itemLimit) throws FirefoxDataException {
        return getCollectionSync(new GetCollectionCall<List<PasswordRecord>>() {
            @Override
            public void getCollectionAsync(final OnSyncComplete<List<PasswordRecord>> onComplete) {
                FirefoxSyncPasswords.getBlocking(syncConfig, itemLimit, onComplete);
            }
        });
    }

    @NonNull
    @Override
    public DataCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxDataException {
        return getHistory(-1);
    }

    @NonNull
    @Override
    public DataCollectionResult<List<HistoryRecord>> getHistoryWithLimit(final int itemLimit) throws FirefoxDataException {
        return getHistory(itemLimit);
    }

    @NonNull
    private DataCollectionResult<List<HistoryRecord>> getHistory(final int itemLimit) throws FirefoxDataException {
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
    private <T> DataCollectionResult<T> getCollectionSync(final GetCollectionCall<T> getCollectionCall) throws FirefoxDataException {
        try {
            // The get collection calls are actually blocking but w/ delegates (see issue #3) so `makeSync` will only
            // actually time out if the get collection requests time-out. This code is confusing but it works & I
            // didn't have time to clean it up - please clean it up with #3.
            return IOUtils.makeSync(REQUEST_TIMEOUT_MILLIS, new IOUtils.AsyncCall<DataCollectionResult<T>>() {
                @Override
                public void initAsyncCall(final IOUtils.OnAsyncCallComplete<DataCollectionResult<T>> onComplete) {
                    getCollectionCall.getCollectionAsync(new OnSyncComplete<T>() {
                        @Override public void onSuccess(final DataCollectionResult<T> result) { onComplete.onComplete(result); }
                        @Override public void onException(final FirefoxDataException e) { onComplete.onException(e); }
                    });
                }
            });
        } catch (final ExecutionException e) {
            throw new FirefoxDataException("Exception occurred during request.", e);
        } catch (final TimeoutException e) {
            throw new FirefoxDataException("Request timed out.", e);
        }
    }

    @NonNull
    @Override
    public String getEmail() throws FirefoxDataException {
        return account.email; // We cache the email but it can change (issue #11).
    }

    private interface GetCollectionCall<T> {
        void getCollectionAsync(final OnSyncComplete<T> onComplete);
    }
}
