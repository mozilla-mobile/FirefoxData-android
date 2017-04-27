/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.login;

import android.content.Context;
import android.support.annotation.WorkerThread;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountShared;
import org.mozilla.accounts.login.FirefoxAccountLoginMarriedDelegate.MarriedCallback;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.State;

import java.lang.ref.WeakReference;

public class FirefoxAccountLoginUtils {
    private FirefoxAccountLoginUtils() {}

    /**
     * Attempts to advance the given account's state to Married, storing the account on success.
     * Results will be returned through the given callback.
     */
    public static void advanceStateToMarried(final Context context, final FirefoxAccount account, final MarriedCallback marriedCallback) {
        // TODO (optimization): If account is already married, we have no need to spawn a runnable.
        // - advance uses the network & must be called from a background thread.
        // - We make AdvanceToMarriedRunnable non-anonymous in order to prevent leaking the Context.
        FirefoxAccountShared.executor.execute(new AdvanceToMarriedRunnable(context, account, marriedCallback));
    }

    // We make this non-anonymous in order to prevent leaking Context.
    private static class AdvanceToMarriedRunnable implements Runnable {
        private final WeakReference<Context> contextWeakReference;
        private final FirefoxAccount account;
        private final FirefoxAccountLoginMarriedDelegate.MarriedCallback callback;

        public AdvanceToMarriedRunnable(final Context context, final FirefoxAccount account, final MarriedCallback callback) {
            this.contextWeakReference = new WeakReference<>(context);
            this.account = account;
            this.callback = callback;
        }

        @WorkerThread
        @Override
        public void run() {
            new FxAccountLoginStateMachine().advance(account.accountState, State.StateLabel.Married,
                    new FirefoxAccountLoginMarriedDelegate(contextWeakReference.get(), account, callback));

        }
    }
}
