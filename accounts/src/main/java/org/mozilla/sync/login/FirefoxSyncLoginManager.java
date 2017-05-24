/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mozilla.sync.FirefoxSyncClient;

/**
 * A interface to let a user log into a FirefoxSync account or manage an account if the user has
 * already signed in.
 *
 * Callers should always call {@link #onActivityResult(int, int, Intent)} from their corresponding
 * {@link Activity#onActivityResult(int, int, Intent)} method to receive the results of a
 * {@link #promptLogin(Activity, String, LoginCallback)}.
 *
 * Retrieve an instance via the main {@link org.mozilla.sync.FirefoxSync} entry point.
 */
public interface FirefoxSyncLoginManager { // todo SessionManager? AccountException? SessionException?

    /**
     * Prompts the user to log in, makes a few additional network requests to set up their account, and calls the given
     * callback.
     *
     * This method can be called from any thread. The callback will be called on a background thread private to
     * FirefoxSync, which is an acceptable place to call the blocking get collection methods of {@link FirefoxSyncClient}.
     *
     * @param activity The Activity from which we're prompting for log in.
     * @param callerName The name of your application, which is shown in the user's Firefox Account dashboard to identify this account session.
     * @param callback The methods to call on completion.
     */
    @AnyThread void promptLogin(final Activity activity, String callerName, @NonNull LoginCallback callback);

    /**
     * Attempts to load a stored account and calls the given callback with the results. On success, additional network
     * calls may be made to set the account up for Sync before the callback is called.
     *
     * This method can be called from any thread. The callback will be called on a background thread private to
     * FirefoxSync, which is an acceptable place to call the blocking get collection methods of {@link FirefoxSyncClient}.
     *
     * @param callback The methods to call on completion.
     */
    @AnyThread void loadStoredSyncAccount(@NonNull LoginCallback callback);

    /**
     * Returns a boolean whether or not the user has signed into a still-valid Firefox Sync account.
     *
     * This method may be called from any thread.
     *
     * @return true if there is a valid account, false otherwise.
     */
    @AnyThread boolean isSignedIn();

    /**
     * Signs the user out of the stored account, if it exists.
     *
     * This metod may be called from any thread.
     */
    @AnyThread void signOut();

    /**
     * Handles the results of a {@link #promptLogin(Activity, String, LoginCallback)} call: this method <b>must</b> be
     * called from {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

    /** A callback for {@link org.mozilla.sync.FirefoxSyncClient} request. */
    interface LoginCallback { // TODO: AccountLoginCallback & AccountCallback?
        /** Called when a {@link org.mozilla.sync.FirefoxSyncClient} has been successfully retrieved. */
        void onSuccess(FirefoxSyncClient syncClient);

        /**
         * Called when there was a failure to retrieve a {@link FirefoxSyncClient}. There can be many causes
         * such as network failure, failure to load a stored account from disk, account credentials that have
         * expired, etc.
         *
         * If a stored account is no longer valid, it will be deleted from disk before this method is called, and
         * {@link #isSignedIn()} will return false.
         *
         * @param e The Exception with which this failure occurred; this is provided for easier debugging.
         */
        void onFailure(FirefoxSyncLoginException e);

        /** Called when a user has cancelled a login prompt. */
        void onUserCancel();
    }
}
