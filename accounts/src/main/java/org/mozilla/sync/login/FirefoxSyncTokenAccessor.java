/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.support.annotation.WorkerThread;
import org.mozilla.sync.impl.FirefoxSyncAssertionException;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxSyncShared;
import org.mozilla.sync.impl.FirefoxAccountUtils;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

/** A static class that provides functions to retrieve sync tokens. */
class FirefoxSyncTokenAccessor {

    private FirefoxSyncTokenAccessor() {}

    // todo: document threading.
    // TODO: mention when assertions are returned.
    /**
     * Gets a Sync Token or returns an error through the provided callback. The given account
     * <b>must</b> be in the Married state.
     *
     * The request is made from the calling thread but the callback runs on the TODO thread.
     * This is unintuitive (see issue #3).
     *
     * @throws IllegalStateException if the account is not in the Married state.
     */
    @WorkerThread // network request.
    public static void getBlocking(final FirefoxAccount account, final FirefoxSyncTokenServerClientDelegate callback) {
        if (!FirefoxAccountUtils.isMarried(account.accountState)) {
            callback.handleError(new FirefoxSyncAssertionException("Assertion failed: expected account to be in married state. Instead: " +
                    account.accountState.getStateLabel().name()));
            return;
        }
        final Married marriedState = FirefoxAccountUtils.getMarried(account.accountState);

        final URI tokenServerURI = account.endpointConfig.syncConfig.tokenServerURL;
        final String assertion;
        try {
            assertion = marriedState.generateAssertion(FxAccountUtils.getAudienceForURL(tokenServerURI.toString()),
                    JSONWebTokenUtils.DEFAULT_ASSERTION_ISSUER);
        } catch (final URISyntaxException e) {
            // Should never happen: occurs when tokenServerURI is cannot be parsed to a URI but it originally comes from a URI.
            callback.handleError(new FirefoxSyncAssertionException("Assertion failed: hard-coded tokenServerURI String cannot be parsed to String"));
            return;
        } catch (final GeneralSecurityException | IOException | NonObjectJSONException e) {
            callback.handleError(new Exception("Unable to create token server assertion.", e));
            return;
        }

        final TokenServerClient tokenServerClient = new TokenServerClient(tokenServerURI, FirefoxSyncShared.executor); // TODO: do we actually want to use this executor? What about networkExecutor?
        tokenServerClient.getTokenFromBrowserIDAssertion(assertion, true, marriedState.getClientState(),
                callback);
    }

    /** A base implementation of {@link TokenServerClientDelegate} that provides a user agent. */
    static abstract class FirefoxSyncTokenServerClientDelegate implements TokenServerClientDelegate {
        @Override public final String getUserAgent() { return FxAccountConstants.USER_AGENT; } // todo: set.
    }
}
