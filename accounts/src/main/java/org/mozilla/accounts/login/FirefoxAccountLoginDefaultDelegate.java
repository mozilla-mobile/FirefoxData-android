/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.login;

import android.content.Context;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.FirefoxAccountShared;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.StateFactory;

import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

/**
 * A login state machine delegate that provides a default configuration and stores an updated account configuration.
 */
public abstract class FirefoxAccountLoginDefaultDelegate implements FxAccountLoginStateMachine.LoginStateMachineDelegate {

    protected static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    protected final FirefoxAccount account;
    private final Executor networkExecutor;

    public FirefoxAccountLoginDefaultDelegate(final FirefoxAccount account, final Executor networkExecutor) {
        this.account = account;
        this.networkExecutor = networkExecutor;
    }

    @Override
    public FxAccountClient getClient() {
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
