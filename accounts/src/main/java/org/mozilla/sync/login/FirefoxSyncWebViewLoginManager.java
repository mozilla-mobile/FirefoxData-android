/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.util.SparseArray;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxSyncShared;
import org.mozilla.sync.sync.FirefoxSyncClient;
import org.mozilla.sync.sync.InternalFirefoxSyncClientFactory;

import static org.mozilla.sync.impl.FirefoxSyncShared.LOGTAG;

/**
 * A {@link FirefoxSyncLoginManager} implementation that uses the a native Android
 * web view & the FxA web sign in flow to log in.
 */
class FirefoxSyncWebViewLoginManager implements FirefoxSyncLoginManager {

    private final FirefoxAccountSessionSharedPrefsStore sessionStore;

    /** Temp storage of args to {@link #promptLogin(Activity, String, LoginCallback)} for use in {@link #onActivityResult(int, int, Intent)}. */
    private final SparseArray<PromptLoginArgs> requestCodeToPromptLoginArgs = new SparseArray<>();
    private int nextRequestCode = 3561; // arbitrary.

    FirefoxSyncWebViewLoginManager(final FirefoxAccountSessionSharedPrefsStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public boolean isSignedIn() {
        try {
            sessionStore.loadSession(); // If returns without throwing, we're signed in.
            return true;
        } catch (final FirefoxAccountSessionSharedPrefsStore.FailedToLoadSessionException e) {
            return false;
        }
    }

    @Override
    public void promptLogin(final Activity activity, @NonNull final String callerName, @NonNull final LoginCallback callback) {
        if (callback == null || callerName == null) { throw new IllegalArgumentException("Expected callback & callerName to be non-null"); }

        requestCodeToPromptLoginArgs.put(nextRequestCode, new PromptLoginArgs(callerName, callback));

        final Intent loginIntent = new Intent(activity, FirefoxSyncWebViewLoginActivity.class);
        //loginIntent.putExtra(FirefoxSyncWebViewLoginActivity.EXTRA_DEBUG_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getStage()); // Uncomment for dev purposes.
        activity.startActivityForResult(loginIntent, nextRequestCode);
        nextRequestCode += 1;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (!isActivityResultOurs(requestCode, data)) { return; }

        final PromptLoginArgs promptLoginArgs = requestCodeToPromptLoginArgs.get(requestCode);
        requestCodeToPromptLoginArgs.delete(requestCode);
        if (promptLoginArgs == null) { throw new IllegalStateException("Did not have callback for given request code: " + requestCode); }

        switch (resultCode) {
            case FirefoxSyncWebViewLoginActivity.RESULT_OK:
                onActivityResultOK(data, promptLoginArgs.callerName, promptLoginArgs.callback);
                break;

            case FirefoxSyncWebViewLoginActivity.RESULT_ERROR:
                onActivityResultError(data, promptLoginArgs.callback);
                break;

            case FirefoxSyncWebViewLoginActivity.RESULT_CANCELED:
                FirefoxSyncLoginShared.executor.execute(new Runnable() { // all callbacks from background thread.
                    @Override public void run() { promptLoginArgs.callback.onUserCancel(); }
                });
                break;
        }
    }

    /**
     * Handles a successful login.
     *
     * At the time of writing (5/18/17), the several network calls this method (and the methods it calls) makes have
     * their time-out duration specified in their override of {@link BaseResourceDelegate#connectionTimeout()} & friends.
     */
    private void onActivityResultOK(@NonNull final Intent data, final String callerName, final LoginCallback callback) {
        final FirefoxAccount firefoxAccount = data.getParcelableExtra(FirefoxSyncWebViewLoginActivity.EXTRA_ACCOUNT);

        // This generally is aligned with whether or not we have a session signed in. However, we need to make the marriage
        // request (proposal? ;) before we can save a session and for that, we need a user agent, which needs a set
        // application name - set it here and undo it if we fail to create a session.
        FirefoxSyncShared.setSessionApplicationName(callerName); // HACK: see function javadoc for info.

        // Account must be married to do anything useful with Sync.
        FirefoxAccountUtils.advanceAccountToMarried(firefoxAccount, FirefoxSyncLoginShared.executor, new FirefoxAccountUtils.MarriedLoginCallback() {
            @Override
            public void onMarried(final Married marriedState) {
                final FirefoxAccount updatedAccount = firefoxAccount.withNewState(marriedState);
                final FirefoxAccountSession session = new FirefoxAccountSession(updatedAccount, callerName);
                sessionStore.saveSession(session);
                prepareSyncClientAndCallback(session.firefoxAccount, callback);
            }

            @Override
            public void onNotMarried(final State notMarriedState) {
                // We failed to marry the account and start a session: reset the application name associated with the failed session.
                FirefoxSyncShared.setSessionApplicationName(null); // HACK: see function javadoc for info.

                // TODO: onUserCancel? Maybe verify verified state before advanceToMarried.
                final String failureMessage = (!notMarriedState.verified) ?
                    "Account needs to be verified to access Sync data." :
                    "Account failed to advance to Married state for unknown reason"; // Unfortunately, we otherwise can't figure out why an advance failed. :(
                callback.onFailure(FirefoxSyncLoginException.newWithoutThrowable(failureMessage));
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
                        loginCallback.onFailure(FirefoxSyncLoginException.newWithoutThrowable(
                                "Server does not contain crypto keys: it is likely the user has not uploaded data to the server"));
                    }

                    @Override
                    public void onRequestFailure(final Exception e) {
                        // TODO: maybe we need to delete account.
                        loginCallback.onFailure(new FirefoxSyncLoginException("Request to access crypto keys failed", e));
                    }

                    @Override
                    public void onError(final Exception e) {
                        // todo: this fails (I think b/c I borked account) but now we're stuck.
                        loginCallback.onFailure(new FirefoxSyncLoginException("Unable to create crypto keys request.", e));
                    }
                });
            }

            @Override
            public void handleFailure(final TokenServerException e) { // Received response but unable to obtain taken.
                if (e instanceof TokenServerException.TokenServerInvalidCredentialsException) {
                    // The credentials don't work with the TokenServer so we're going to have to prompt again.
                    Log.w(LOGTAG, "Login credentials are considered invalid with token server: deleting stored account.");
                    sessionStore.deleteStoredSession();
                }

                loginCallback.onFailure(new FirefoxSyncLoginException("Sync token response does not contain valid token", e));
            }

            @Override
            public void handleError(final Exception e) { // Error connecting.
                loginCallback.onFailure(new FirefoxSyncLoginException("Error connecting to sync token server.", e));
            }

            @Override
            public void handleBackoff(final int backoffSeconds) {
                loginCallback.onFailure(FirefoxSyncLoginException.newWithoutThrowable(
                        "Sync token server requested backoff of " + backoffSeconds + " seconds."));
            }
        });
    }

    private void onActivityResultError(@NonNull final Intent data, final LoginCallback callback) {
        final String failureReason = data.getStringExtra(FirefoxSyncWebViewLoginActivity.EXTRA_FAILURE_REASON);
        FirefoxSyncLoginShared.executor.execute(new Runnable() { // All callbacks on background thread.
            @Override
            public void run() {
                callback.onFailure(FirefoxSyncLoginException.newWithoutThrowable(
                        "WebViewLoginActivity returned error: " + failureReason));
            }
        });
    }

    private boolean isActivityResultOurs(final int requestCode, @Nullable final Intent data) {
        if (data == null) { return false; }
        final String action = data.getAction();
        return (action != null &&
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
                    callback.onFailure(new FirefoxSyncLoginException("Failed to restore account from disk.", e));
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
        final FirefoxAccountSession session;
        try {
            session = sessionStore.loadSession();
        } catch (final FirefoxAccountSessionSharedPrefsStore.FailedToLoadSessionException e) {
            Log.w(LOGTAG, "signOut: failed to load account. Does the account exist? Ignoring sign out request."); // don't log exception for personal info.
            return;
        }
        sessionStore.deleteStoredSession();

        // The user agent for the destroy request is derived from the session application name, which we're about to unset.
        final String userAgent = FirefoxSyncShared.getUserAgent();

        // Our session has ended: we no longer have a signed in application and don't need its name.
        FirefoxSyncShared.setSessionApplicationName(null); // HACK: see function javadoc for more info.

        FirefoxSyncLoginShared.executor.execute(new Runnable() {
            @Override
            public void run() {
                // If the request fails, the session won't be destroyed. We don't want to the application developer to
                // have to handle making another request so we should add library code to make the request on failure
                // (issue #10).
                FirefoxAccountUtils.destroyAccountSession(session.firefoxAccount, new FirefoxAccountUtils.DestroySessionResourceDelegate(userAgent) {
                    private static final String LOG_PREFIX = "destroyAccountSession: ";

                    @Override
                    public void handleHttpResponse(final HttpResponse response) {
                        if (response.getStatusLine().getStatusCode() == 200) {
                            Log.d(LOGTAG, LOG_PREFIX + "success!");
                        } else {
                            Log.w(LOGTAG, LOG_PREFIX + "HTTP response is failure! " + response.getStatusLine());
                        }
                    }

                    @Override
                    void onFailure(final Exception e) {
                        Log.e(LOGTAG, LOG_PREFIX + "unable to complete request."); // don't log e for potential PII.
                    }
                });
            }
        });
    }

    private static class PromptLoginArgs {
        final String callerName;
        final LoginCallback callback;

        private PromptLoginArgs(final String callerName, final LoginCallback callback) {
            if (callerName == null || callback == null) { throw new IllegalStateException("Expected callerName and callback to be non-null."); }
            this.callerName = callerName;
            this.callback = callback;
        }
    }
}
