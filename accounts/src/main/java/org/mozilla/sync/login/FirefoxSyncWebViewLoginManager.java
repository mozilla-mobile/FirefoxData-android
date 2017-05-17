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
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.FirefoxSyncLoginManager;
import org.mozilla.sync.login.FirefoxSyncLoginException.FailureReason;
import org.mozilla.sync.impl.FirefoxAccountUtils;
import org.mozilla.sync.sync.InternalFirefoxSyncClientFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mozilla.sync.impl.FirefoxAccountShared.LOGTAG;

/**
 * TODO: docs.
 */
class FirefoxSyncWebViewLoginManager implements FirefoxSyncLoginManager {
    private static final int REQUEST_CODE = 3561; // arbitrary.

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(); // TODO: use shared executor?
    private final FirefoxAccountSharedPrefsStore accountStore;

    // Values stored between the login call & `onActivityResult` so we can execute the given callback.
    private String requestCallerName;
    private LoginCallback requestLoginCallback;

    FirefoxSyncWebViewLoginManager(final Context context) {
        this.accountStore = new FirefoxAccountSharedPrefsStore(context);
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

        final Intent loginIntent = new Intent(activity, FirefoxAccountWebViewLoginActivity.class);
        //loginIntent.putExtra(FirefoxAccountWebViewLoginActivity.EXTRA_DEBUG_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getStage()); // Uncomment for dev purposes.
        activity.startActivityForResult(loginIntent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (!isActivityResultOurs(requestCode, data)) { return; }
        // TODO: can onActivityResult be called multiple times? If so, we should keep {requestCode:callback} instead of throwing.
        if (requestCallerName == null) { throw new IllegalStateException("onActivityResult unexpectedly called more than once for one promptLogin call (or promptLogin was never called)."); }

        switch (resultCode) {
            case FirefoxAccountWebViewLoginActivity.RESULT_OK:
                onActivityResultOK(data);
                break;

            case FirefoxAccountWebViewLoginActivity.RESULT_ERROR:
                onActivityResultError(data);
                break;

            case FirefoxAccountWebViewLoginActivity.RESULT_CANCELED:
                requestLoginCallback.onUserCancel();
                break;
        }

        requestCallerName = null;
        requestLoginCallback = null;
    }

    private void onActivityResultOK(@NonNull final Intent data) {
        final FirefoxAccount firefoxAccount = data.getParcelableExtra(FirefoxAccountWebViewLoginActivity.EXTRA_ACCOUNT);

        // Keep references because they'll be nulled before the async call completes.
        final String requestCallerName = this.requestCallerName; // TODO: use in user agent.
        final LoginCallback requestLoginCallback = this.requestLoginCallback;

        // Account must be married to do anything useful with Sync.
        FirefoxAccountUtils.advanceAccountToMarried(firefoxAccount, backgroundExecutor, new FirefoxAccountUtils.MarriedLoginCallback() {
            @Override
            public void onMarried(final Married marriedState) {
                final FirefoxAccount updatedAccount = firefoxAccount.withNewState(marriedState);
                accountStore.saveFirefoxAccount(updatedAccount); // todo: callback threads.
                prepareSyncClientAndCallback(updatedAccount, requestLoginCallback);
            }

            @Override
            public void onNotMarried(final State notMarriedState) {
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

    @WorkerThread // calls to network.
    private void prepareSyncClientAndCallback(final FirefoxAccount marriedAccount, final LoginCallback loginCallback) {
        // todo: assert married?
        FirefoxSyncTokenAccessor.get(marriedAccount, new FirefoxSyncTokenAccessor.TokenCallback() {
            @Override
            public void onTokenReceived(final TokenServerToken token) {
                FirefoxSyncCryptoKeysAccessor.get(marriedAccount, token, new FirefoxSyncCryptoKeysAccessor.CollectionKeysCallback() {
                    @Override
                    public void onKeysReceived(final CollectionKeys collectionKeys) {
                        final FirefoxSyncClient syncClient = InternalFirefoxSyncClientFactory.getSyncClient(marriedAccount, token, collectionKeys);
                        loginCallback.onSuccess(syncClient); // TODO: callback threads; here & below.
                    }

                    @Override
                    public void onException(final Exception e) {
                        loginCallback.onFailure(new FirefoxSyncLoginException(e, FailureReason.FAILED_TO_LOAD_ACCOUNT)); // todo more specific.
                    }
                });
            }

            @Override
            public void onError(final Exception e) {
                loginCallback.onFailure(new FirefoxSyncLoginException(e, FailureReason.FAILED_TO_LOAD_ACCOUNT)); // todo more specific?
            }
        });
    }

    private void onActivityResultError(@NonNull final Intent data) {
        final String failureStr = data.getStringExtra(FirefoxAccountWebViewLoginActivity.EXTRA_FAILURE_REASON);
        FailureReason failureReason;
        try {
            failureReason = failureStr != null ? FailureReason.valueOf(failureStr) : FailureReason.UNKNOWN;
        } catch (final IllegalArgumentException e) {
            Log.e(LOGTAG, "onActivityResultError: WebViewLoginActivity returned invalid failure reason.");
            failureReason = FailureReason.UNKNOWN;
        }
        requestLoginCallback.onFailure(new FirefoxSyncLoginException("WebViewLoginActivity returned failure", failureReason));
    }

    private boolean isActivityResultOurs(final int requestCode, @Nullable final Intent data) {
        if (data == null) { return false; }
        final String action = data.getAction();
        return (requestCode == REQUEST_CODE &&
                action != null &&
                // Another Activity can use the same request code so we verify the Intent data too.
                action.equals(FirefoxAccountWebViewLoginActivity.ACTION_WEB_VIEW_LOGIN_RETURN));
    }

    @Override
    public void loadStoredSyncAccount(@NonNull final LoginCallback callback) {
        if (callback == null) { throw new IllegalArgumentException("Expected callback to be non-null."); }

        final FirefoxAccount account;
        try {
            account = accountStore.loadFirefoxAccount();
        } catch (final FirefoxAccountSharedPrefsStore.FailedToLoadAccountException e) {
            callback.onFailure(new FirefoxSyncLoginException(e, FailureReason.FAILED_TO_LOAD_ACCOUNT));
            return;
        }

        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                prepareSyncClientAndCallback(account, callback);
            }
        });
    }

    @Override
    public void signOut() {
        accountStore.removeFirefoxAccount(); // todo: test me!
        // TODO: hit API: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#post-v1accountdevicedestroy
    }
}
