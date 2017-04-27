/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountShared;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

// TODO: not public.
/**
 * A static class that provides functions to retrieve sync tokens.
 */
public class FirefoxAccountSyncTokenAccessor {

    public interface TokenCallback {
        void onError(Exception e);
        void onTokenReceived(TokenServerToken token);
    }

    /**
     * Gets a Sync Token or returns an error through the provided callback. The given account
     * <b>must</b> be in the Married state.
     *
     * @throws IllegalStateException if the account is not in the Married state.
     */
    public static void get(final FirefoxAccount account, final TokenCallback callback) {
        assertMarried(account);

        final Married marriedState = (Married) account.accountState;
        final URI tokenServerURI = account.endpointConfig.syncConfig.tokenServerURL;
        final String assertion;
        try {
            assertion = marriedState.generateAssertion(FxAccountUtils.getAudienceForURL(tokenServerURI.toString()),
                    JSONWebTokenUtils.DEFAULT_ASSERTION_ISSUER);
        } catch (final NonObjectJSONException | IOException | GeneralSecurityException | URISyntaxException e) {
            callback.onError(e);
            return;
        }

        final TokenServerClient tokenServerClient = new TokenServerClient(tokenServerURI, FirefoxAccountShared.executor);
        tokenServerClient.getTokenFromBrowserIDAssertion(assertion, true, marriedState.getClientState(),
                new FirefoxAccountTokenServerClientDelegate(callback));
    }

    private static void assertMarried(final FirefoxAccount account) {
        if (!(account.accountState instanceof Married)) {
            throw new IllegalStateException("Expected account to be in the Married state. Instead: " +
                    account.accountState.getClass().getSimpleName());
        }
    }

    private static class FirefoxAccountTokenServerClientDelegate implements TokenServerClientDelegate {

        private final TokenCallback callback;

        private FirefoxAccountTokenServerClientDelegate(final TokenCallback callback) {
            this.callback = callback;
        }

        @Override
        public void handleSuccess(final TokenServerToken token) {
            callback.onTokenReceived(token);
        }

        @Override
        public void handleFailure(final TokenServerException e) {
            callback.onError(e);
        }

        @Override
        public void handleError(final Exception e) {
            callback.onError(e);
        }

        @Override
        public void handleBackoff(final int backoffSeconds) {
            // The API will expect to retrieve history immediately so for simplicity, we'll just
            // call a backoff an error for now.
            callback.onError(new Exception("Received requested backoff of " + backoffSeconds + " seconds. " +
                    "For simplicity, we implement this as an error."));
        }

        @Override public String getUserAgent() { return FxAccountConstants.USER_AGENT; }
    }
}
