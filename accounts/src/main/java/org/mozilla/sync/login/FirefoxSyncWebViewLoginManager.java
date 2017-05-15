/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.FirefoxSyncLoginManager;
import org.mozilla.sync.FirefoxSyncLoginException;
import org.mozilla.sync.FirefoxSyncLoginException.FailureReason;
import org.mozilla.sync.impl.FirefoxAccountUtils;
import org.mozilla.sync.sync.InternalFirefoxSyncClientFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO: docs.
 */
class FirefoxSyncWebViewLoginManager implements FirefoxSyncLoginManager {
    private static final int REQUEST_CODE = 3561; // arbitrary.

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(); // TODO: use shared executor?
    private final FirefoxAccountDevelopmentStore accountStore;

    // Values stored between the login call & `onActivityResult` so we can execute the given callback.
    private String requestCallerName;
    private LoginCallback requestLoginCallback;

    FirefoxSyncWebViewLoginManager(final Context context) {
        this.accountStore = new FirefoxAccountDevelopmentStore(context);
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
        //loginIntent.putExtra(FirefoxAccountWebViewLoginActivity.EXTRA_DEBUG_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getStage()); // todo: RM me for non-debug.
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
                accountStore.saveFirefoxAccount(updatedAccount);

                final FirefoxSyncClient syncClient = InternalFirefoxSyncClientFactory.getSyncClient(updatedAccount);
                requestLoginCallback.onSuccess(syncClient); // TODO: callback threads; here & below.
            }

            @Override
            public void onNotMarried(final State notMarriedState) {
                final FailureReason failureReason;
                if (!notMarriedState.verified) {
                    failureReason = FailureReason.ACCOUNT_NOT_VERIFIED;
                } else {
                    failureReason = FailureReason.UNKNOWN; // Unfortunately, we can't figure out why an advance failed. :(
                }
                requestLoginCallback.onFailure(new FirefoxSyncLoginException(failureReason));
            }
        });
    }

    private void onActivityResultError(@NonNull final Intent data) {
        final String failureStr = data.getStringExtra(FirefoxAccountWebViewLoginActivity.EXTRA_FAILURE_REASON);
        final FailureReason failureReason = failureStr != null ? FailureReason.valueOf(failureStr) : FailureReason.UNKNOWN;
        requestLoginCallback.onFailure(new FirefoxSyncLoginException(failureReason));
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

        final FirefoxAccount account = accountStore.loadFirefoxAccount();
        if (account == null) {
            callback.onFailure(new FirefoxSyncLoginException(FailureReason.UNKNOWN)); // todo: can we get more specific errors? Or maybe just intended lib user action - Exception causes handle specifics.
            return;
        }

        final FirefoxSyncClient syncClient = InternalFirefoxSyncClientFactory.getSyncClient(account);
        callback.onSuccess(syncClient);
    }

    @Override
    public void signOut() {
        // TODO: clear development store, hit API: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#post-v1accountdevicedestroy
    }
}
