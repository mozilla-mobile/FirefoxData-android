/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.mozilla.sync.login.FirefoxAccountWebViewLoginActivity;

/**
 * TODO: docs.
 */
class FirefoxSyncWebViewLoginManager implements FirefoxSyncLoginManager {
    private static final int REQUEST_CODE = 3561;

    // TODO: explain.
    private String requestCallerName; // TODO: who sends? Client or LoginManager?
    private LoginCallback requestLoginCallback;

    // TODO: callback always async? here and below
    @Override
    public void promptLogin(final Activity activity, final String callerName, final LoginCallback callback) {
        // TODO: ensure not called already.
        requestCallerName = callerName;
        requestLoginCallback = callback;

        final Intent loginIntent = new Intent(activity, FirefoxAccountWebViewLoginActivity.class);
        loginIntent.putExtra(FirefoxAccountWebViewLoginActivity.EXTRA_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getProduction());
        activity.startActivityForResult(loginIntent, REQUEST_CODE);
    }

    // TODO: test verification state.
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) { // todo: BOOLEAN TO HANDLE?
        if (!isActivityResultOurs(requestCode, data)) { return; }

        switch (resultCode) {
            case FirefoxAccountWebViewLoginActivity.RESULT_OK:
                final FirefoxAccount firefoxAccount = data.getParcelableExtra(FirefoxAccountWebViewLoginActivity.EXTRA_ACCOUNT);
                // TODO: when married? When set caller name?
                // TODO: verrified?
                // TODO: persist account or whole sync client?
                final FirefoxSyncClient syncClient = new FirefoxSyncClientImpl(firefoxAccount);
                requestLoginCallback.onSuccess(syncClient);
                break;

            case FirefoxAccountWebViewLoginActivity.RESULT_ERROR:
            case FirefoxAccountWebViewLoginActivity.RESULT_CANCELED:
                requestLoginCallback.onFailure(new LoginSyncException(null)); // todo
                break;
        }

        requestCallerName = null;
        requestLoginCallback = null;
    }

    private boolean isActivityResultOurs(final int requestCode, final Intent data) {
        final String action = data.getAction();
        return (requestCode == REQUEST_CODE &&
                action != null &&
                action.equals(FirefoxAccountWebViewLoginActivity.ACTION_RETURN_FIREFOX_ACCOUNT)); // todo: explain
    }

    @Override
    public void loadStoredSyncAccount(final Context context, final LoginCallback callback) {
        final FirefoxAccount account = new FirefoxAccountDevelopmentStore(context).loadFirefoxAccount();
        if (account == null) {
            callback.onFailure(new LoginSyncException(LoginSyncException.FailureReason.UNABLE_TO_LOAD_ACCOUNT));
            return;
        }

        final FirefoxSyncClient syncClient = new FirefoxSyncClientImpl(account);
        callback.onSuccess(syncClient);
    }

    @Override
    public void signOut() {
        // TODO: clear development store, hit API: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#post-v1accountdevicedestroy
    }
}
