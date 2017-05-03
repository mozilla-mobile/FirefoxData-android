/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.support.annotation.Nullable;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.FirefoxAccountSyncUtils;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.util.ChainableCallable;
import org.mozilla.util.ChainableCallable.ChainableCallableWithCallback;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Container file for SyncClientCommand classes. */
public class SyncClientCommands {
    private SyncClientCommands() {}

    /** A base-class for commands accessing collections from sync  - these are expected to have callbacks. */
    public static abstract class SyncClientCollectionCommand<R> extends ChainableCallableWithCallback<FirefoxAccountSyncConfig> {
        protected final SyncCollectionCallback<R> callback;

        protected SyncClientCollectionCommand(final SyncCollectionCallback<R> callback) {
            super(callback);
            this.callback = callback;
        }

        /**
         * Convenience method to make a get request to the given collection.
         *
         * @param collectionArgs The arguments for this get request. Note that "full=1" is included as a default arg.
         * @param delegate The callback for the request.
         */
        protected static void makeGetRequestForCollection(final FirefoxAccountSyncConfig syncConfig, final String collectionName,
                @Nullable final Map<String, String> collectionArgs, final SyncClientBaseResourceDelegate delegate) throws URISyntaxException {
            final Map<String, String> allArgs = getDefaultArgs();
            if (collectionArgs != null) { allArgs.putAll(collectionArgs); }

            final URI uri = FirefoxAccountSyncUtils.getCollectionURI(syncConfig.token, collectionName, null, allArgs);
            final BaseResource resource = new BaseResource(uri);
            resource.delegate = delegate;
            resource.get();
        }

        private static Map<String, String> getDefaultArgs() {
            final Map<String, String> args = new HashMap<String, String>();
            args.put("full", "1"); // get full data, not just IDs.
            return args;
        }
    }

    /** A helper for handling pre-commands that begin an async request and need to block until completion. */
    public static abstract class SyncClientAsyncPreCommand extends ChainableCallable<FirefoxAccountSyncConfig> {
        private static final long ASYNC_TIMEOUT_SECONDS = 60;

        @Override
        public final FirefoxAccountSyncConfig call(final FirefoxAccountSyncConfig syncConfig) throws Exception {
            final CountDownLatch makeSynchronousLatch = new CountDownLatch(1);
            final ReturnValueContainer returnValueContainer = new ReturnValueContainer();

            initAsyncCall(syncConfig, new OnAsyncPreCommandComplete() {
                @Override
                public void onException(final Exception e) {
                    returnValueContainer.exception = e;
                    makeSynchronousLatch.countDown();
                }

                @Override
                public void onSuccess(final FirefoxAccountSyncConfig updatedSyncConfig) {
                    returnValueContainer.updatedSyncConfig = updatedSyncConfig;
                    makeSynchronousLatch.countDown();
                }
            });

            final boolean countDownReachedZero = makeSynchronousLatch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS); // Await end of async operation.
            if (!countDownReachedZero) { throw new Exception("Async command timed out."); }
            else if (returnValueContainer.exception != null) { throw returnValueContainer.exception; }
            return returnValueContainer.updatedSyncConfig;
        }

        /**
         * Begins the async call associated with this pre-command.
         * @param onComplete When the async call is completed, one of the methods should be called.
         */
        abstract void initAsyncCall(FirefoxAccountSyncConfig syncConfig, OnAsyncPreCommandComplete onComplete) throws Exception;

        private static class ReturnValueContainer {
            private Exception exception;
            private FirefoxAccountSyncConfig updatedSyncConfig;
        }
    }

    protected interface OnAsyncPreCommandComplete {
        void onException(Exception e);
        void onSuccess(FirefoxAccountSyncConfig updatedSyncConfig);
    }
}
