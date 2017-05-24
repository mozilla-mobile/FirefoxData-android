/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.FirefoxSyncLoginManager;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxSyncShared;
import org.mozilla.sync.login.FirefoxSyncLoginException.FailureReason;
import org.mozilla.sync.sync.InternalFirefoxSyncClientFactory;

import static org.mozilla.sync.impl.FirefoxSyncShared.LOGTAG;

/**
 * TODO: docs.
 */
class FirefoxSyncWebViewLoginManager implements FirefoxSyncLoginManager {
    private static final int REQUEST_CODE = 3561; // arbitrary.

    private final FirefoxAccountSessionSharedPrefsStore sessionStore;

    // Values stored between `promptLogin` & `onActivityResult` so we can execute the given callback.
    //
    // These are static to prevent API misuse: if a user gets a LoginManager instance for promptLogin,
    // they could get a different instance for `onActivityResult`. With static vars, we don't have
    // this issue. This is a little janky and doesn't protect new LoginManager implementations but I
    // found it better than the alternatives: 1) make the entire API static, which restricts new
    // implementation flexibility or 2) pass the callback to the getLoginManager call, which makes
    // it harder for the caller to see where the callbacks are set.
    private static String requestCallerName;
    private static LoginCallback requestLoginCallback;

    FirefoxSyncWebViewLoginManager(final Context context) {
        this.sessionStore = new FirefoxAccountSessionSharedPrefsStore(context);
    }

    @Override
    public void promptLogin(final Activity activity, final String callerName, @NonNull final LoginCallback callback) {
        if (callback == null) { throw new IllegalArgumentException("Expected callback to be non-null"); }
        if (requestCallerName != null) {
            throw new IllegalStateException("promptLogin unexpectedly called twice before the result was returned. " +
                    "Did you call onActivityResult?");
        }

        requestCallerName = callerName;
        requestLoginCallback = callback;

        final Intent loginIntent = new Intent(activity, FirefoxSyncWebViewLoginActivity.class);
        //loginIntent.putExtra(FirefoxSyncWebViewLoginActivity.EXTRA_DEBUG_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getStage()); // Uncomment for dev purposes.
        activity.startActivityForResult(loginIntent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (!isActivityResultOurs(requestCode, data)) { return; }
        if (requestCallerName == null) { throw new IllegalStateException("onActivityResult unexpectedly called more " +
                "than once for one promptLogin call (or promptLogin was never called)."); }

        switch (resultCode) {
            case FirefoxSyncWebViewLoginActivity.RESULT_OK:
                onActivityResultOK(data);
                break;

            case FirefoxSyncWebViewLoginActivity.RESULT_ERROR:
                onActivityResultError(data);
                break;

            case FirefoxSyncWebViewLoginActivity.RESULT_CANCELED:
                final LoginCallback requestLoginCallback = FirefoxSyncWebViewLoginManager.requestLoginCallback; // nulled before callback would run.
                FirefoxSyncLoginShared.executor.execute(new Runnable() { // all callbacks from background thread.
                    @Override public void run() { requestLoginCallback.onUserCancel(); }
                });
                break;
        }

        requestCallerName = null;
        requestLoginCallback = null;
    }

    /**
     * Handles a successful login.
     *
     * At the time of writing (5/18/17), the several network calls this method (and the methods it calls) makes have
     * their time-out duration specified in their override of {@link BaseResourceDelegate#connectionTimeout()} & friends.
     */
    private void onActivityResultOK(@NonNull final Intent data) {
        final FirefoxAccount firefoxAccount = data.getParcelableExtra(FirefoxSyncWebViewLoginActivity.EXTRA_ACCOUNT);

        // This generally is aligned with whether or not we have a session signed in. However, we need to make the marriage
        // request (proposal? ;) before we can save a session and for that, we need a user agent, which needs a set
        // application name - set it here and undo it if we fail to create a session.
        FirefoxSyncShared.setSessionApplicationName(requestCallerName); // HACK: see function javadoc for info.

        // Keep references because they'll be nulled before the async call completes.
        final String requestCallerName = FirefoxSyncWebViewLoginManager.requestCallerName;
        final LoginCallback requestLoginCallback = FirefoxSyncWebViewLoginManager.requestLoginCallback;

        // Account must be married to do anything useful with Sync.
        FirefoxAccountUtils.advanceAccountToMarried(firefoxAccount, FirefoxSyncLoginShared.executor, new FirefoxAccountUtils.MarriedLoginCallback() {
            @Override
            public void onMarried(final Married marriedState) {
                final FirefoxAccount updatedAccount = firefoxAccount.withNewState(marriedState);
                final FirefoxAccountSession session = new FirefoxAccountSession(updatedAccount, requestCallerName);
                sessionStore.saveSession(session);
                prepareSyncClientAndCallback(session.firefoxAccount, requestLoginCallback);
            }

            @Override
            public void onNotMarried(final State notMarriedState) {
                // We failed to marry the account and start a session: reset the application name associated with the failed session.
                FirefoxSyncShared.setSessionApplicationName(null); // HACK: see function javadoc for info.

                final FailureReason failureReason;
                if (!notMarriedState.verified) {
                    failureReason = FailureReason.ACCOUNT_NEEDS_VERIFICATION;
                } else {
                    failureReason = FailureReason.UNKNOWN; // Unfortunately, we otherwise can't figure out why an advance failed. :(
                }
                requestLoginCallback.onFailure(new FirefoxSyncLoginException("Unable to move to Married account state", failureReason));
            }
        });
    }

    /**
     * Gets the account ready to be used in a Sync Client.
     *
     * Note that the API declares that all callbacks (including errors!) should occur on one of our private background
     * threads.
     */
    @WorkerThread // calls to network.
    private void prepareSyncClientAndCallback(final FirefoxAccount marriedAccount, final LoginCallback loginCallback) {
        FirefoxAccountUtils.assertIsMarried(marriedAccount.accountState);
        FirefoxSyncTokenAccessor.getBlocking(marriedAccount, new FirefoxSyncTokenAccessor.FirefoxSyncTokenServerClientDelegate() {
            @Override
            public void handleSuccess(final TokenServerToken token) {
                FirefoxSyncCryptoKeysAccessor.getBlocking(marriedAccount, token, new FirefoxSyncCryptoKeysAccessor.CollectionKeysCallback() {
                    @Override
                    public void onKeysReceived(final CollectionKeys collectionKeys) {
                        final FirefoxSyncClient syncClient = InternalFirefoxSyncClientFactory.getSyncClient(marriedAccount, token, collectionKeys);
                        loginCallback.onSuccess(syncClient);
                    }

                    @Override
                    public void onKeysDoNotExist() {
                        loginCallback.onFailure(new FirefoxSyncLoginException("Server does not contain crypto keys: " +
                                "it is likely the user has not uploaded data to the server", FailureReason.USER_HAS_NO_LINKED_DEVICES));
                    }

                    @Override
                    public void onRequestFailure(final Exception e) {
                        loginCallback.onFailure(new FirefoxSyncLoginException(e, FailureReason.SERVER_ERROR));
                    }

                    @Override
                    public void onError(final Exception e) {
                        final FailureReason failureReason = (e instanceof FirefoxSyncAssertionException) ?
                                FailureReason.ASSERTION_FAILURE : FailureReason.NETWORK_ERROR;
                        loginCallback.onFailure(new FirefoxSyncLoginException(e, failureReason));
                    }
                });
            }

            @Override
            public void handleFailure(final TokenServerException e) { // Received response but unable to obtain taken.
                final FailureReason failureReason;
                if (e instanceof TokenServerException.TokenServerMalformedRequestException ||
                        e instanceof TokenServerException.TokenServerMalformedResponseException) {
                    failureReason = FailureReason.ASSERTION_FAILURE;
                } else if (e instanceof TokenServerException.TokenServerUnknownServiceException) {
                    // 404 from TokenServer: this could be programmer error or server error.
                    failureReason = FailureReason.SERVER_ERROR;
                } else if (e instanceof TokenServerException.TokenServerInvalidCredentialsException) {
                    failureReason = FailureReason.REQUIRES_LOGIN_PROMPT;
                } else {
                    // I couldn't figure out what TokenServerConditionsRequiredException is and the base
                    // TokenServerException should never be thrown.
                    failureReason = FailureReason.UNKNOWN;
                }
                loginCallback.onFailure(new FirefoxSyncLoginException(e, failureReason));
            }

            @Override
            public void handleError(final Exception e) { // Error connecting.
                final FailureReason failureReason = (e instanceof FirefoxSyncAssertionException) ?
                        FailureReason.ASSERTION_FAILURE : FailureReason.NETWORK_ERROR;
                loginCallback.onFailure(new FirefoxSyncLoginException(e, failureReason));
            }

            @Override
            public void handleBackoff(final int backoffSeconds) {
                loginCallback.onFailure(FirefoxSyncLoginException.newForBackoffSeconds(backoffSeconds));
            }
        });
    }

    private void onActivityResultError(@NonNull final Intent data) {
        final String failureStr = data.getStringExtra(FirefoxSyncWebViewLoginActivity.EXTRA_FAILURE_REASON);
        FailureReason failureReason;
        try {
            failureReason = failureStr != null ? FailureReason.valueOf(failureStr) : FailureReason.UNKNOWN;
        } catch (final IllegalArgumentException e) {
            Log.e(LOGTAG, "onActivityResultError: WebViewLoginActivity returned invalid failure reason.");
            failureReason = FailureReason.UNKNOWN;
        }

        final FailureReason finalFailureReason = failureReason; // Couldn't find a good way to express this.
        final LoginCallback requestLoginCallback = FirefoxSyncWebViewLoginManager.requestLoginCallback; // nulled before callback runs.
        FirefoxSyncLoginShared.executor.execute(new Runnable() { // All callbacks on background thread.
            @Override
            public void run() {
                requestLoginCallback.onFailure(new FirefoxSyncLoginException("WebViewLoginActivity returned failure", finalFailureReason));
            }
        });
    }

    private boolean isActivityResultOurs(final int requestCode, @Nullable final Intent data) {
        if (data == null) { return false; }
        final String action = data.getAction();
        return (requestCode == REQUEST_CODE &&
                action != null &&
                // Another Activity can use the same request code so we verify the Intent data too.
                action.equals(FirefoxSyncWebViewLoginActivity.ACTION_WEB_VIEW_LOGIN_RETURN));
    }

    @Override
    public void loadStoredSyncAccount(@NonNull final LoginCallback callback) {
        if (callback == null) { throw new IllegalArgumentException("Expected callback to be non-null."); }

        // The callback should always be called from the background thread so we just do everything on the background thread.
        FirefoxSyncLoginShared.executor.execute(new Runnable() {
            @Override
            public void run() {
                final FirefoxAccountSession session;
                try {
                    session = sessionStore.loadSession();
                } catch (final FirefoxAccountSessionSharedPrefsStore.FailedToLoadSessionException e) {
                    callback.onFailure(new FirefoxSyncLoginException(e, FailureReason.REQUIRES_LOGIN_PROMPT));
                    return;
                }

                // This may be the first time the session is loaded for this application run so set the application name.
                FirefoxSyncShared.setSessionApplicationName(session.applicationName); // HACK: see function javadoc for more info.
                prepareSyncClientAndCallback(session.firefoxAccount, callback);
            }
        });
    }

    @Override
    public void signOut() {
        sessionStore.deleteStoredSession();

        // Our session has ended: we no longer have a signed in application and don't need its name.
        FirefoxSyncShared.setSessionApplicationName(null); // HACK: see function javadoc for more info.

        // TODO: test me & hit API: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#post-v1accountdevicedestroy
    }
}
