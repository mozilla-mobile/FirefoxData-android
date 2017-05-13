/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * TODO:
 */
public interface FirefoxSyncLoginManager {
    // TODO: method names; docs. which thread callbacks called from?
    void promptLogin(final Activity activity, String callerName, @NonNull LoginCallback callback);
    void loadStoredSyncAccount(@NonNull LoginCallback callback);
    void signOut();

    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

    interface LoginCallback {
        void onSuccess(FirefoxSyncClient syncClient);
        void onFailure(FirefoxSyncLoginException e);
        void onUserCancel();
    }
}
