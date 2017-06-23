/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.login;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mozilla.fxa_data.FirefoxData;
import org.mozilla.fxa_data.FirefoxDataException;
import org.mozilla.fxa_data.download.FirefoxDataClient;

/**
 * A interface to let a user log into a FirefoxData account or manage an account if the user has
 * already signed in.
 *
 * Callers should always call {@link #onActivityResult(int, int, Intent)} from their corresponding
 * {@link Activity#onActivityResult(int, int, Intent)} method to receive the results of a
 * {@link #promptLogin(Activity, String, LoginCallback)}.
 *
 * Retrieve an instance via the main {@link FirefoxData} entry point.
 *
 * This class is not thread-safe.
 */
public interface FirefoxDataLoginManager {

    /**
     * Prompts the user to log in, makes a few additional network requests to set up their account, and calls the given
     * callback. On success, the account will be stored, {@link #isSignedIn()} will return true, and subsequent accesses
     * to the Sync account should use {@link #loadStoredAccount(LoginCallback)}.
     *
     * Caveat: this method can fail before, or after, an account is stored (for example, a network error occurs after
     * logging in to Firefox Accounts but before we retrieve the separate Sync credentials for the SyncClient).
     * {@link #isSignedIn()} can be used to determine if an account was stored, even if this method reports a failure.
     *
     * This method can be called from any thread. The callback will be called on a background thread private to
     * FirefoxData, which is an acceptable place to call the blocking get collection methods of {@link FirefoxDataClient}.
     *
     * @param activity The Activity from which we're prompting for log in.
     * @param callerName The name of your application, which is shown in the user's Firefox Account dashboard to identify this account session.
     * @param callback The methods to call on completion.
     */
    @AnyThread void promptLogin(final Activity activity, String callerName, @NonNull LoginCallback callback);

    /**
     * Attempts to load a stored account and calls the given callback with the results without interrupting the user. On
     * success, additional network calls may be made to set the account up for Sync before the callback is called.
     * {@link #promptLogin(Activity, String, LoginCallback)} is expected to have been called at least once before this
     * method is called.
     *
     * If an account is stored and can be loaded, {@link #isSignedIn()} will return true. If during sign in, the account
     * is determined to be no longer valid, the stored account will be deleted and {@link #isSignedIn()} will begin to
     * return false.
     *
     * This method can be called from any thread. The callback will be called on a background thread private to
     * FirefoxData, which is an acceptable place to call the blocking get collection methods of {@link FirefoxDataClient}.
     *
     * @param callback The methods to call on completion.
     */
    @AnyThread void loadStoredAccount(@NonNull LoginCallback callback);

    /**
     * Returns a boolean whether or not the user has signed into a still-valid Firefox Sync account.
     *
     * If this method returns true, {@link #loadStoredAccount(LoginCallback)}. Otherwise,
     * one should consider using {@link #promptLogin(Activity, String, LoginCallback)}.
     *
     * This method may be called from any thread.
     *
     * @return true if there is a valid account, false otherwise.
     */
    @AnyThread boolean isSignedIn();

    /**
     * Signs the user out of the stored account, if it exists.
     *
     * This method may be called from any thread.
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

    /** A callback for {@link FirefoxDataClient} request. */
    interface LoginCallback {
        /** Called when a {@link FirefoxDataClient} has been successfully retrieved. */
        void onSuccess(FirefoxDataClient dataClient);

        /**
         * Called when there was a failure to retrieve a {@link FirefoxDataClient}. There can be many causes
         * such as network failure, failure to load a stored account from disk, account credentials that have
         * expired, etc.
         *
         * If a stored account is no longer valid, it will be deleted from disk before this method is called, and
         * {@link #isSignedIn()} will return false.
         *
         * @param e The Exception with which this failure occurred; this is provided for easier debugging.
         */
        void onFailure(FirefoxDataException e);

        /** Called when a user has cancelled a login prompt. */
        void onUserCancel();
    }
}
