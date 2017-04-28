/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.util.ChainableCallable;

import java.util.concurrent.CountDownLatch;

/** Base class for Sync pre commands. */
public abstract class SyncClientPreCommand extends ChainableCallable<FirefoxAccountSyncConfig> {

    /** Helper for handling pre-commands that begin an async request and need to block until completion. */
    static abstract class SyncClientAsyncPreCommand extends SyncClientPreCommand {
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

            makeSynchronousLatch.await(); // Await end of async operation. TODO: timeout?
            if (returnValueContainer.exception != null) { throw returnValueContainer.exception; }
            return returnValueContainer.updatedSyncConfig;
        }

        /**
         * Begins the async call associated with this precommand.
         * @param onComplete When the async call is completed, one of the methods should be called.
         */
        abstract void initAsyncCall(FirefoxAccountSyncConfig syncConfig, OnAsyncPreCommandComplete onComplete);

        private static class ReturnValueContainer {
            protected Exception exception;
            protected FirefoxAccountSyncConfig updatedSyncConfig;
        }
    }

    protected interface OnAsyncPreCommandComplete {
        void onException(Exception e);
        void onSuccess(FirefoxAccountSyncConfig updatedSyncConfig);
    }
}
