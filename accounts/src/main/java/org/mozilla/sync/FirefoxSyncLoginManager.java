/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/**
 * TODO:
 */
public interface FirefoxSyncLoginManager {
    // TODO: method names; docs.
    void promptLogin(final Activity activity, String callerName, LoginCallback callback);
    void loadStoredSyncAccount(final Context context, LoginCallback callback);
    void signOut();

    void onActivityResult(int requestCode, int resultCode, Intent data);

    interface LoginCallback {
        void onSuccess(FirefoxSyncClient syncClient);
        void onFailure(LoginSyncException e);
        // TODO: onCancel?
    }
}
