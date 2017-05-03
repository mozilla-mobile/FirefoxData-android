/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts;

import android.support.annotation.CheckResult;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.fxa.login.Engaged;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.Utils;

/**
 * Data class representing a Firefox Account.
 *
 * This implementation is independent from Firefox for Android's because we want to be able to quickly access read-only
 * Sync data in a standalone library but AndroidFxAccount is heavily dependent on Android Accounts. To use Android
 * Accounts properly, we should have all Firefox Account apps accessing the same Account APK from Google Play, which is
 * beyond the scope of this implementation, considering we want to have it running quickly.
 */
public class FirefoxAccount {

    private static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    public final String email;
    public final String uid;

    public final State accountState;
    public final FirefoxAccountEndpointConfig endpointConfig;

    public FirefoxAccount(final String email, final String uid, final State state,
            final FirefoxAccountEndpointConfig endpointConfig) {
        this.email = email;
        this.uid = uid;
        this.accountState = state;
        this.endpointConfig = endpointConfig;
    }

    /**
     * Gets a new account instance with the updated state - does *not*
     * modify the existing account instance.
     *
     * @return the account with the given state.
     */
    @CheckResult
    public FirefoxAccount withNewState(final State newState) {
        return new FirefoxAccount(email, uid, newState, endpointConfig);
    }

    /**
     * Returns a Firefox Account from the web sign in flow.
     *
     * @param jsonData The stringified JSON of the "event.details.data" object.
     * @return the account associated with the JSON or null if an error occurs.
     */
    public static FirefoxAccount fromWebFlow(final FirefoxAccountEndpointConfig endpointConfig, final String jsonData) {
        if (TextUtils.isEmpty(jsonData) || jsonData.equals("undefined")) {
            Log.e(LOGTAG, "fromWebFlow: input empty.");
            return null;
        }

        final JSONObject data;
        try {
            data = new JSONObject(jsonData);
        } catch (final JSONException e) {
            Log.e(LOGTAG, "fromWebFlow: unable to parse json."); // Don't log exception to avoid leaking user data.
            return null;
        }

        try {
            final String uid = data.getString("uid");
            final String email = data.getString("email");
            final boolean verified = data.optBoolean("verified", false);
            final byte[] keyFetchToken = Utils.hex2Byte(data.getString("keyFetchToken"));
            final byte[] sessionToken = Utils.hex2Byte(data.getString("sessionToken"));
            final byte[] unwrapBKey = Utils.hex2Byte(data.getString("unwrapBKey"));

            final State accountState = new Engaged(email, uid, verified, unwrapBKey, sessionToken, keyFetchToken);
            return new FirefoxAccount(email, uid, accountState, endpointConfig);
        } catch (final JSONException e) {
            Log.d(LOGTAG, "fromWebFlow: unable to gather all necessary fields from json."); // Don't log exception to avoid leaking user data.
            return null;
        }
    }
}
