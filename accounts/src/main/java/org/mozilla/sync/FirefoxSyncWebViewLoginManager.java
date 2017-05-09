/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mozilla.sync.LoginSyncException.FailureReason;
import org.mozilla.sync.login.FirefoxAccountWebViewLoginActivity;

/**
 * TODO: docs.
 */
class FirefoxSyncWebViewLoginManager implements FirefoxSyncLoginManager {
    private static final int REQUEST_CODE = 3561; // arbitrary.

    // TODO: explain.
    private String requestCallerName; // TODO: who sends? Client or LoginManager?
    private LoginCallback requestLoginCallback;

    @Override
    public void promptLogin(final Activity activity, final String callerName, @NonNull final LoginCallback callback) {
        if (callback == null) { throw new IllegalArgumentException("Expected callback to be non-null"); }

        // TODO: ensure not called already.
        requestCallerName = callerName;
        requestLoginCallback = callback;

        final Intent loginIntent = new Intent(activity, FirefoxAccountWebViewLoginActivity.class);
        loginIntent.putExtra(FirefoxAccountWebViewLoginActivity.EXTRA_DEBUG_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getStage());
        activity.startActivityForResult(loginIntent, REQUEST_CODE);
    }

    // TODO: test verification state.
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) { // todo: BOOLEAN TO HANDLE?
        if (!isActivityResultOurs(requestCode, data)) { return; }

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
        // TODO: when married? When set caller name?
        // TODO: verrified?
        // TODO: persist account or whole sync client?
        final FirefoxSyncClient syncClient = new FirefoxSyncFirefoxAccountClient(firefoxAccount);
        requestLoginCallback.onSuccess(syncClient);
    }

    private void onActivityResultError(@NonNull final Intent data) {
        final String failureStr = data.getStringExtra(FirefoxAccountWebViewLoginActivity.EXTRA_FAILURE_REASON);
        final FailureReason failureReason = failureStr != null ? FailureReason.valueOf(failureStr) : FailureReason.UNKNOWN;
        requestLoginCallback.onFailure(new LoginSyncException(failureReason));
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
    public void loadStoredSyncAccount(final Context context, @NonNull final LoginCallback callback) {
        if (callback == null) { throw new IllegalArgumentException("Expected callback to be non-null."); }

        final FirefoxAccount account = new FirefoxAccountDevelopmentStore(context).loadFirefoxAccount();
        if (account == null) {
            callback.onFailure(new LoginSyncException(FailureReason.UNKNOWN)); // todo
            return;
        }

        final FirefoxSyncClient syncClient = new FirefoxSyncFirefoxAccountClient(account);
        callback.onSuccess(syncClient);
    }

    @Override
    public void signOut() {
        // TODO: clear development store, hit API: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#post-v1accountdevicedestroy
    }
}
