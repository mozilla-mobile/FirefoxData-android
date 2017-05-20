/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mozilla.sync.login.FirefoxSyncLoginException;

/**
 * TODO:
 */
public interface FirefoxSyncLoginManager {
    // TODO: method names; docs.

    /**
     * Prompts the user to log in, makes a few additional network requests to set up their account, and calls the given
     * callback.
     *
     * This method can be called from any thread. The callback will be called on a background thread private to
     * FirefoxSync, which is an acceptable place to call the blocking get collection methods of {@link FirefoxSyncClient}.
     *
     * @param activity
     * @param callerName
     * @param callback
     */
    @AnyThread void promptLogin(final Activity activity, String callerName, @NonNull LoginCallback callback);

    /**
     * Attempts to load a stored account and calls the given callback with the results. On success, additional network
     * calls will be made to set the account up for Sync before the callback is called.
     *
     * This method can be called from any thread. The callback will be called on a background thread private to
     * FirefoxSync, which is an acceptable place to call the blocking get collection methods of {@link FirefoxSyncClient}.
     *
     * @param callback
     */
    @AnyThread void loadStoredSyncAccount(@NonNull LoginCallback callback);
    void signOut();

    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

    interface LoginCallback { // TODO: AccountLoginCallback & AccountCallback?
        void onSuccess(FirefoxSyncClient syncClient);
        void onFailure(FirefoxSyncLoginException e); // TODO: AccountException? SessionException?
        void onUserCancel();
    }
}
