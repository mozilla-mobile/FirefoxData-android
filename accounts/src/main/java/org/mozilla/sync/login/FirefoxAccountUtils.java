/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.util.Log;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.fxa.login.StateFactory;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxSyncShared;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

/**
 * A collection of functions for operating on a FirefoxAccount.
 */
class FirefoxAccountUtils {
    private FirefoxAccountUtils() {}

    static void assertIsMarried(final State accountState) {
        if (!isMarried(accountState)) {
            throw new IllegalStateException("Expected the given account state to be Married. " +
                    "Instead: " + accountState.getStateLabel().toString());
        }
    }

    static boolean isMarried(final State accountState) {
        return accountState.getStateLabel() == StateLabel.Married;
    }

    /**
     * Asserts Married and returns the account state casted to Married.
     *
     * @return the given account state casted to {@link Married}.
     * @throws IllegalArgumentException if the account is not in the Married state.
     */
    static Married getMarried(final State accountState) {
        if (!isMarried(accountState)) {
            throw new IllegalArgumentException("Expected account to be married.");
        }
        return (Married) accountState;
    }

    /**
     * Advances the given account to the married state.
     *
     * Both the network request and the callback will run on the given Executor.
     */
    static void advanceAccountToMarried(final FirefoxAccount firefoxAccount, final Executor backgroundExecutor, final MarriedLoginCallback callback) {
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

    /**
     * A login state machine delegate that provides a default configuration and stores an updated account configuration.
     */
    abstract static class FirefoxAccountLoginDefaultDelegate implements FxAccountLoginStateMachine.LoginStateMachineDelegate {

        protected static final String LOGTAG = FirefoxSyncShared.LOGTAG;

        protected final FirefoxAccount account;
        private final Executor networkExecutor;

        FirefoxAccountLoginDefaultDelegate(final FirefoxAccount account, final Executor networkExecutor) {
            this.account = account;
            this.networkExecutor = networkExecutor;
        }

        @Override
        public FxAccountClient getClient() {
            // TODO: Set user agent in account client to proposed value https://github.com/mozilla/fxa-auth-server/issues/1889
            return new FxAccountClient20(account.endpointConfig.authServerURL.toString(), networkExecutor);
        }

        @Override
        public void handleTransition(final FxAccountLoginTransition.Transition transition, final State state) {
            Log.d(LOGTAG, "transitioning: " + transition + " - " + state.getStateLabel().toString());
        }

        @Override
        public BrowserIDKeyPair generateKeyPair() throws NoSuchAlgorithmException { return StateFactory.generateKeyPair(); }

        // The values below are from existing Delegate implementations - I'm not sure why these values are chosen.
        @Override
        public long getCertificateDurationInMilliseconds() { return 12 * 60 * 60 * 1000; }

        @Override
        public long getAssertionDurationInMilliseconds() { return 15 * 60 * 1000; }
    }
}
