/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.login;

import android.content.Context;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.StateFactory;

import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;

/**
 * TODO:
 * - name (what does this delegate do? Better than defaultDelegate?)
 * - description.
 */
public class FirefoxAccountLoginStateMachineDelegate implements FxAccountLoginStateMachine.LoginStateMachineDelegate {

    public interface LoginHandler {
        void handleNotMarried(State notMarriedState);
        void handleMarried(FirefoxAccount account, Married married);
    }

    private static final String LOGTAG = "FirefoxAccountLoginStat";

    private final WeakReference<Context> contextWeakReference;
    private final FirefoxAccount account;
    private final LoginHandler loginHandler;

    public FirefoxAccountLoginStateMachineDelegate(final Context context, final FirefoxAccount account,
            final LoginHandler loginHandler) {
        this.contextWeakReference = new WeakReference<Context>(context);
        this.account = account;
        this.loginHandler = loginHandler;
    }

    @Override
    public FxAccountClient getClient() {
        return new FxAccountClient20(account.endpointConfig.authServerURL.toString(), Executors.newSingleThreadExecutor());
    }

    @Override
    public void handleTransition(final FxAccountLoginTransition.Transition transition, final State state) {
        Log.d(LOGTAG, "transitioning: " + transition + " - " + state.getStateLabel().toString());
    }

    // TODO: Separate store from handleMarried/notMarried.
    @Override
    public void handleFinal(final State state) {
        if (!(state instanceof Married)) {
            Log.w(LOGTAG, "Unable to get to Married.");
            loginHandler.handleNotMarried(state);
            return;
        }
        final Married marriedState = (Married) state;

        final Context context = contextWeakReference.get();
        if (context == null) {
            Log.w(LOGTAG, "Unable to save advanced account: context is null.");
            // TODO
            return;
        }
        // TODO: should block? I guess shared prefs isn't blocking.
        final FirefoxAccount updatedAccount = account.withNewState(marriedState);
        new FirefoxAccountDevelopmentStore(context).saveFirefoxAccount(updatedAccount);
        Log.d("lol", "newAccount! " + state);

        Log.d(LOGTAG, "handleFinal: handlingMarried");
        loginHandler.handleMarried(updatedAccount, marriedState);
        // TODO: move this behavior to composition?
        // TODO: this delegate does one thing.
    }

    @Override
    public BrowserIDKeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return StateFactory.generateKeyPair();
    }

    // TODO: values below are from FxADefaultLoginStateMachineDelegate. Why are they that way?
    @Override
    public long getCertificateDurationInMilliseconds() { return 12 * 60 * 60 * 1000; }

    @Override
    public long getAssertionDurationInMilliseconds() { return 15 * 60 * 1000; }
}
