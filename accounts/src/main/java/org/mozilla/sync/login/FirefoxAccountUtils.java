/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.support.annotation.WorkerThread;
import android.util.Log;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.fxa.login.StateFactory;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.HKDF;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxSyncRequestUtils;
import org.mozilla.sync.impl.FirefoxSyncShared;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

import static org.mozilla.sync.impl.FirefoxSyncShared.LOGTAG;

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

        protected final FirefoxAccount account;
        private final Executor networkExecutor;

        FirefoxAccountLoginDefaultDelegate(final FirefoxAccount account, final Executor networkExecutor) {
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

    /**
     * Destroys the session token associated with this account by invalidating it with the remote
     * server. After calling this method, it is expected any references, especially persisted
     * references, to the given {@link FirefoxAccount} will be deleted.
     *
     * This method speaks the API endpoint specified by:
     *   https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#post-sessiondestroy
     *
     * This implementation is inspired by implementations in {@link FxAccountClient20}.
     *
     * @param account The account to delete.
     * @param delegate The callback for when this function completes.
     */
    @WorkerThread
    static void destroyAccountSession(final FirefoxAccount account, final DestroySessionResourceDelegate delegate) {
        final byte[] sessionToken;
        try {
            sessionToken = account.accountState.getSessionToken();
        } catch (final State.NotASessionTokenState e) {
            Log.w(LOGTAG, "The given account to destroy the session token of is not in a state with " +
                    "a session token. Instead: " + account.accountState.getStateLabel() +
                    ". Ignoring request.");
            delegate.onFailure(e);
            return;
        }

        final byte[] tokenId = new byte[32];
        final byte[] reqHMACKey = new byte[32];
        final byte[] requestKey = new byte[32];
        try {
            HKDF.deriveMany(sessionToken, new byte[0], FxAccountUtils.KW("sessionToken"), tokenId, reqHMACKey, requestKey);
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            Log.e(LOGTAG, "destroyAccountSession: failed to derive keys."); // don't log exception for PII.
            delegate.onFailure(e);
            return;
        }

        final URI serverURI;
        try {
            serverURI = new URI(account.endpointConfig.authServerURL.toString() + "/session/destroy");
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Hard-coded URI strings failed to parse!", e);
        }

        final BaseResource resource = new BaseResource(serverURI);
        delegate.setResources(serverURI, tokenId, reqHMACKey);
        resource.delegate = delegate;
        resource.postBlocking(new ExtendedJSONObject()); // No arguments.
    }

    abstract static class DestroySessionResourceDelegate implements ResourceDelegate {

        // Taken from SyncBaseResourceDelegate.
        private static final int CONNECTION_TIMEOUT_MILLIS = 1000 * 30;
        private static final int SOCKET_TIMEOUT_MILLIS = 1000 * 2 * 60;

        // We get this with an instance, rather than statically like the rest of the code, because we're destroying a
        // session and its application name, but this call is async so we can't block to delete the application name
        // only when it completes.
        private final String userAgent;

        private boolean areResourcesSet = false;
        private URI serverURI;
        private byte[] tokenId;
        private byte[] reqHMACKey;

        DestroySessionResourceDelegate(final String userAgent) {this.userAgent = userAgent;}

        /** <b>Must be called</b>: sets values to identify this request. */
        private void setResources(final URI serverURI, final byte[] tokenId, final byte[] reqHMACKey) {
            this.serverURI = serverURI;
            this.tokenId = tokenId;
            this.reqHMACKey = reqHMACKey;
            areResourcesSet = true;
        }

        abstract void onFailure(Exception e);

        @Override public void addHeaders(final HttpRequestBase request, final DefaultHttpClient client) { }

        @Override
        public AuthHeaderProvider getAuthHeaderProvider() {
            if (!areResourcesSet) { throw new IllegalStateException("Expected setResources to be called before this method."); }
            return FirefoxSyncRequestUtils.getAuthHeaderProvider(serverURI, Utils.byte2Hex(tokenId), reqHMACKey, true);
        }

        @Override public String getUserAgent() { return userAgent; }

        // We don't care to distinguish right now.
        @Override public void handleHttpProtocolException(final ClientProtocolException e) { onFailure(e); }
        @Override public void handleHttpIOException(final IOException e) { onFailure(e); }
        @Override public void handleTransportException(final GeneralSecurityException e) { onFailure(e); }

        @Override public int connectionTimeout() { return CONNECTION_TIMEOUT_MILLIS; }
        @Override public int socketTimeout() { return SOCKET_TIMEOUT_MILLIS; }
    };
}
