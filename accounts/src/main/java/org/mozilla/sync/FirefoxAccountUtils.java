/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.support.annotation.WorkerThread;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.sync.login.FirefoxAccountLoginDefaultDelegate;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

/**
 * A collection of functions for operating on a FirefoxAccount.
 */
public class FirefoxAccountUtils {
    private FirefoxAccountUtils() {}

    public static boolean isMarried(final State accountState) {
        return accountState.getStateLabel() == StateLabel.Married;
    }

    /**
     * Asserts Married and returns the account state casted to Married.
     *
     * @return the given account state casted to {@link Married}.
     * @throws IllegalArgumentException if the account is not in the Married state.
     */
    public static Married getMarried(final State accountState) {
        if (!isMarried(accountState)) {
            throw new IllegalArgumentException("Expected account to be married.");
        }
        return (Married) accountState;
    }

    public static void advanceAccountToMarried(final FirefoxAccount firefoxAccount, final Executor backgroundExecutor, final MarriedLoginCallback callback) {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                new FxAccountLoginStateMachine().advance(firefoxAccount.accountState, StateLabel.Married, new FirefoxAccountLoginDefaultDelegate(firefoxAccount, backgroundExecutor) {
                    @Override
                    public void handleFinal(final State state) {
                        if (isMarried(state)) {
                            callback.onMarried(getMarried(state));
                        } else {
                            callback.onNotMarried(state);
                        }
                    }
                });
            }
        });
    }

    interface MarriedLoginCallback {
        void onMarried(Married marriedState);
        void onNotMarried(State notMarriedState);
    }
}
